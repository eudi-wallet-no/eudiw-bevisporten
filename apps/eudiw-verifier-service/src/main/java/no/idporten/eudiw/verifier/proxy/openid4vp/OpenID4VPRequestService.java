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
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import no.idporten.eudiw.verifier.proxy.VerificationException;
import no.idporten.eudiw.verifier.proxy.config.VerifierProxyProperties;
import no.idporten.eudiw.verifier.proxy.crypto.ECUtils;
import no.idporten.eudiw.verifier.proxy.openid4vp.metadata.ClaimsDescription;
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

@RequiredArgsConstructor
@Service
public class OpenID4VPRequestService {

    private final VerifierProxyProperties verifierProxyProperties;
    private final KeystoreManager keystoreManager;
    private Map<String, String> authorizationRequests = new HashMap<>();

    protected URI createRequestUri(String verifierTransactionId) {
        return UriComponentsBuilder
                .fromUriString(verifierProxyProperties.getExternalBaseUri())
                .pathSegment("openid4vp", "authz-request", verifierTransactionId)
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

    protected URI createAuthorizationRequest(String verifierTransactionId) {
        return UriComponentsBuilder.newInstance()
                .scheme("eudi-openid4vp")
                .host(verifierProxyProperties.getSiop2ClientId())
                .queryParam("client_id", verifierProxyProperties.getClientIdentifierScheme())
                .queryParam("request_uri", createRequestUri(verifierTransactionId).toString())
                .build()
                .toUri();
    }

    public String retrieveAuthorizationRequest(String verifierTransactionId) {
        String authorizationRequest = authorizationRequests.remove(verifierTransactionId);
        if (authorizationRequest == null) {
            throw new VerificationException("invalid_request", "Unknown authorization request");
        }
        return authorizationRequest;
    }

    @SneakyThrows
    public URI createAuthorizationRequest(CredentialConfiguration credentialConfiguration, String verifierTransactionId) {
        JWT vpAuthorizationRequest = makeRequestJwt(credentialConfiguration, verifierTransactionId);
        authorizationRequests.put(verifierTransactionId, vpAuthorizationRequest.serialize());
        URI authorizationRequest = createAuthorizationRequest(verifierTransactionId);
        return authorizationRequest;
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
                .claim("dcql_query", makeDCQLSDJwt(credentialConfiguration, state))
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
    public JSONObject makeDCQLSDJwt(CredentialConfiguration credentialConfig, String id) {
        JSONObject credential = new JSONObject()
                .appendField("id", id)
                .appendField("format", credentialConfig.getFormat())
                .appendField("meta", new JSONObject().appendField(
                        "vct_values", List.of(credentialConfig.getVct())));
        JSONArray claims = new JSONArray();
        for (ClaimsDescription claim : credentialConfig.getCredentialMetadata().getClaimsDescriptions()) {
            claims.appendElement(new JSONObject()
                    .appendField("path", claim.getPath()));
        }
        credential.put("claims", claims);
        JSONObject dcql = new JSONObject().appendField("credentials", new JSONArray().appendElement(credential));
        return dcql;
    }

}
