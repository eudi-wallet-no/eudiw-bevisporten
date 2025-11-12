package no.idporten.eudiw.verifier.proxy.openid4vp;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import no.idporten.eudiw.verifier.proxy.VerificationException;
import no.idporten.eudiw.verifier.proxy.config.VerifierProxyProperties;
import no.idporten.eudiw.verifier.proxy.crypto.ECUtils;
import no.idporten.eudiw.verifier.proxy.openid4vp.metadata.CredentialConfiguration;
import no.idporten.lib.keystore.KeyProvider;
import no.idporten.lib.keystore.KeystoreManager;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Clock;
import java.util.*;

@Service
public class OpenID4VPRequestService {

    private final VerifierProxyProperties verifierProxyProperties;
    private final KeystoreManager keystoreManager;
    private final VerificationTransactionService verificationTransactionService;

    // Cache request_id -> verification_transaction_id
    private Map<String, String> requestId2verificationTransactionId = new HashMap<>();

    public OpenID4VPRequestService(VerifierProxyProperties verifierProxyProperties, KeystoreManager keystoreManager, VerificationTransactionService verificationTransactionService) {
        this.verifierProxyProperties = verifierProxyProperties;
        this.keystoreManager = keystoreManager;
        this.verificationTransactionService = verificationTransactionService;
    }

    protected URI createRequestUri(String requestId) {
        return UriComponentsBuilder
                .fromUriString(verifierProxyProperties.getExternalBaseUri())
                .pathSegment("openid4vp", "authz-request", requestId)
                .build()
                .toUri();
    }

    protected URI createResponseUri(String verifierTransactionId) {
        return UriComponentsBuilder
                .fromUriString(verifierProxyProperties.getExternalBaseUri())
                .pathSegment("openid4vp", "authz-response", verifierTransactionId)
                .build()
                .toUri();
    }

    protected URI createAuthorizationRequest(String requestId) {
        return UriComponentsBuilder.newInstance()
                .scheme("eudi-openid4vp")
                .host(verifierProxyProperties.getSiop2ClientId())
                .queryParam("client_id", verifierProxyProperties.getClientIdentifierScheme())
                .queryParam("request_uri", createRequestUri(requestId).toString())
                .build()
                .toUri();
    }

    @SneakyThrows
    public URI createAuthorizationRequest(CredentialConfiguration credentialConfiguration, String verifierTransactionId) {
        String requestId = UUID.randomUUID().toString();
        requestId2verificationTransactionId.put(requestId, verifierTransactionId);
        return createAuthorizationRequest(requestId);
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
        JWT authorizationRequest = makeRequestJwt(verificationTransaction.getCredentialConfiguration(), verificationTransactionId);
        return authorizationRequest.serialize();
    }

    public JWT makeRequestJwt(CredentialConfiguration credentialConfiguration, String state) throws Exception {
        KeyProvider keyProvider = keystoreManager.getKeyProvider("access");
        List<Base64> certChain = new ArrayList<>();
        certChain.add(Base64.encode(keyProvider.certificate().getEncoded()));
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .audience("https://self-issued.me/v2")
                .issuer(verifierProxyProperties.getExternalBaseUri())
                .claim("response_uri", createResponseUri(state).toString())
                .claim("response_type", "vp_token")
                .claim("response_mode", "direct_post.jwt")
                .claim("nonce", UUID.randomUUID().toString())
                .claim("state", state)
                .claim("client_id", verifierProxyProperties.getClientIdentifierScheme())
                .claim("dcql_query", makeDCQLQuery(credentialConfiguration, state))
                .claim("client_metadata", makeClientMetadata())
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

    @Deprecated
    private JSONObject makeVpFormats() {
        JSONArray algs = new JSONArray();
        algs.add(JWSAlgorithm.RS256.getName());
        algs.add(JWSAlgorithm.ES256.getName());
        algs.add(JWSAlgorithm.ES384.getName());
        JSONObject mdoc = new JSONObject();
        mdoc.appendField("alg", algs);
        JSONObject format = new JSONObject();
        format.appendField("mso_mdoc", mdoc);
        return format;
    }

    private JSONObject makeVpFormatsSupported() {
        JSONObject mdoc = new JSONObject();
        JSONObject format = new JSONObject();
        format.appendField("mso_mdoc", mdoc);
        return format;
    }

    private JSONObject makeClientMetadata() {
        JSONObject metadata = new JSONObject();
        metadata.appendField("jwks", makeJwks().toPublicJWKSet().toJSONObject());
        JSONArray encryptedResponseAlgs = new JSONArray();
        encryptedResponseAlgs.add(EncryptionMethod.A128GCM.getName());
        metadata.appendField("encrypted_response_enc_values_supported", encryptedResponseAlgs);
        metadata.appendField("vp_formats_supported", makeVpFormatsSupported());
        // JARM Android v24
        metadata.appendField("id_token_signed_response_alg", JWSAlgorithm.RS256.getName());
        metadata.appendField("authorization_encrypted_response_alg", JWEAlgorithm.ECDH_ES.getName());
        metadata.appendField("authorization_encrypted_response_enc", EncryptionMethod.A128CBC_HS256.getName());
        // VP formats Android v24
        metadata.appendField("vp_formats", makeVpFormats());
        return metadata;
    }

    @SneakyThrows
    private JWKSet makeJwks() {
        List<JWK> jwkList = new ArrayList<>();
        ECPublicKey publicKey = (ECPublicKey) keystoreManager.getKeyProvider("access").publicKey();
        jwkList.add(new ECKey.Builder(ECUtils.curveFromKey(publicKey), publicKey)
                .keyUse(KeyUse.ENCRYPTION)
                .keyIDFromThumbprint()
                .algorithm(JWEAlgorithm.ECDH_ES)
                .build());
        return new JWKSet(jwkList);
    }

    @SneakyThrows
    public JSONObject makeDCQLQuery(CredentialConfiguration credentialConfiguration, String id) {
        JSONObject credential = new JSONObject()
                .appendField("id", id)
                .appendField("format", credentialConfiguration.getFormat())
                .appendField("meta",
                        "dc+sd-jwt".equals(credentialConfiguration.getFormat())
                                ?
                                new JSONObject().appendField("vct_values", List.of(credentialConfiguration.getVct()))
                                :
                                new JSONObject().appendField("doctype_value", credentialConfiguration.getDoctype()))
                .appendField("claims",
                        credentialConfiguration.getCredentialMetadata().getClaimsDescriptions().stream()
                                .map(cd -> new JSONObject().appendField("path", cd.getPath()))
                                .toList());
        return new JSONObject().appendField("credentials", new JSONArray().appendElement(credential));
    }

}
