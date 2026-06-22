package no.idporten.eudiw.verifier.openid4vp;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.config.ClientApplication;
import no.idporten.eudiw.verifier.config.VerifierServiceProperties;
import no.idporten.eudiw.verifier.crypto.ECUtils;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlQuery;
import no.idporten.lib.keystore.KeyProvider;
import no.idporten.lib.keystore.KeystoreManager;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.security.MessageDigest;
import java.security.interfaces.ECPrivateKey;
import java.time.Clock;
import java.util.*;

@Service
public class OpenID4VPRequestService {

    private final VerifierServiceProperties verifierServiceProperties;
    private final KeystoreManager keystoreManager;
    private final VerificationTransactionService verificationTransactionService;
    private final JsonMapper jsonMapper;

    // Cache request_id -> verification_transaction_id
    private Map<String, String> requestId2verificationTransactionId = new HashMap<>();

    public OpenID4VPRequestService(VerifierServiceProperties verifierServiceProperties, KeystoreManager keystoreManager, VerificationTransactionService verificationTransactionService, JsonMapper jsonMapper) {
        this.verifierServiceProperties = verifierServiceProperties;
        this.keystoreManager = keystoreManager;
        this.verificationTransactionService = verificationTransactionService;
        this.jsonMapper = jsonMapper;
    }

    protected URI createRequestUri(String requestId) {
        return UriComponentsBuilder
                .fromUriString(verifierServiceProperties.getExternalBaseUri())
                .pathSegment("openid4vp", "authz-request", requestId)
                .build()
                .toUri();
    }

    protected URI createResponseUri(String verifierTransactionId) {
        return UriComponentsBuilder
                .fromUriString(verifierServiceProperties.getExternalBaseUri())
                .pathSegment("openid4vp", "authz-response", verifierTransactionId)
                .build()
                .toUri();
    }

    protected URI createOpenID4VPAuthorizationRequest(String requestId, ClientApplication clientApplication) {
        return UriComponentsBuilder.newInstance()
                .scheme("eudi-openid4vp")
                .host(verifierServiceProperties.getSiop2ClientId())
                .queryParam("client_id", makeClientId(clientApplication))
                .queryParam("request_uri", createRequestUri(requestId).toString())
                .build()
                .toUri();
    }

    @SneakyThrows
    private String makeClientId(ClientApplication clientApplication) {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(keystoreManager.getKeyProvider(clientApplication.getKeystoreName()).certificate().getEncoded());
        String clientId = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(md.digest());
        return "x509_hash:" + clientId;
    }

    @SneakyThrows
    public URI createAuthorizationRequest(String verifierTransactionId, ClientApplication clientApplication) {
        String requestId = UUID.randomUUID().toString();
        requestId2verificationTransactionId.put(requestId, verifierTransactionId);
        return createOpenID4VPAuthorizationRequest(requestId, clientApplication);
    }

    @SneakyThrows
    public String retrieveAuthorizationRequest(String requestId) {
        String verificationTransactionId = requestId2verificationTransactionId.remove(requestId);
        if (verificationTransactionId == null) {
            throw new VerificationException("invalid_request", "Unknown authorization request");
        }
        VerificationTransaction verificationTransaction = verificationTransactionService.getVerificationTransaction(verificationTransactionId);
        if (verificationTransaction == null) {
            throw new VerificationException("invalid_request", "Unknown verification transaction");
        }
        String state = UUID.randomUUID().toString();
        JWK encryptionKey = new ECKeyGenerator(Curve.P_256).algorithm(JWEAlgorithm.ECDH_ES).keyUse(KeyUse.ENCRYPTION).keyIDFromThumbprint(true).generate();
        verificationTransaction.setState(state);
        verificationTransaction.setEncryptionKey(encryptionKey);
        JWT authorizationRequest = makeRequestJwt(verificationTransaction, verificationTransactionId);
        return authorizationRequest.serialize();
    }

    private JWT makeRequestJwt(VerificationTransaction verificationTransaction, String verificationTransactionId) throws Exception {
        KeyProvider keyProvider = keystoreManager.getKeyProvider(verificationTransaction.getClientApplication().getKeystoreName());
        List<Base64> certChain = new ArrayList<>();
        certChain.add(Base64.encode(keyProvider.certificate().getEncoded()));
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .audience("https://self-issued.me/v2")
                .issuer(verifierServiceProperties.getExternalBaseUri())
                .claim("response_uri", createResponseUri(verificationTransactionId).toString())
                .claim("response_type", "vp_token")
                .claim("response_mode", "direct_post.jwt")
                .claim("nonce", UUID.randomUUID().toString())
                .claim("state", verificationTransaction.getState())
                .claim("client_id", makeClientId(verificationTransaction.getClientApplication()))
                .claim("dcql_query", convertDcqlQuery(verificationTransaction.getDcqlQuery()))
                .claim("client_metadata", makeClientMetadata(verificationTransaction.getEncryptionKey()))
                .jwtID(UUID.randomUUID().toString()) // Must be unique for each grant
                .issueTime(new Date(Clock.systemUTC().millis())) // Use UTC time!
                .expirationTime(new Date(Clock.systemUTC().millis() + 120000));
        JWTClaimsSet claims = builder.build();
        final JWSHeader jwtHeader;
        final JWSSigner signer;
        jwtHeader = new JWSHeader.Builder(ECUtils.jwsAlgorithmFromKey(keyProvider.publicKey()))
                .x509CertChain(certChain)
                .type(new JOSEObjectType("oauth-authz-req+jwt"))
                .build();
        signer = new ECDSASigner((ECPrivateKey) keyProvider.privateKey());
        SignedJWT signedJWT = new SignedJWT(jwtHeader, claims);
        signedJWT.sign(signer);
        return signedJWT;
    }

    private Map<String, Object> convertDcqlQuery(DcqlQuery dcqlQuery) {
        return jsonMapper.convertValue(dcqlQuery, new HashMap<String, Object>().getClass());
    }


    private JSONObject makeVpFormatsSupported() {
        JSONObject mdoc = new JSONObject();
        JSONObject format = new JSONObject();
        format.appendField("mso_mdoc", mdoc);
        JSONObject sdJwt = new JSONObject();
        sdJwt.appendField("sd-jwt_alg_values", List.of("ES256", "ES384"));
        sdJwt.appendField("kb-jwt_alg_values", List.of("ES256", "ES384"));
        format.appendField("dc+sd-jwt", sdJwt);
        return format;
    }

    private JSONObject makeClientMetadata(JWK encryptionKey) {
        JSONObject metadata = new JSONObject();
        metadata.appendField("jwks", new JWKSet(encryptionKey).toPublicJWKSet().toJSONObject());
        JSONArray encryptedResponseAlgs = new JSONArray();
        encryptedResponseAlgs.add(EncryptionMethod.A128GCM.getName());
        encryptedResponseAlgs.add(EncryptionMethod.A256GCM.getName());
        metadata.appendField("encrypted_response_enc_values_supported", encryptedResponseAlgs);
        metadata.appendField("vp_formats_supported", makeVpFormatsSupported());
        return metadata;
    }

}
