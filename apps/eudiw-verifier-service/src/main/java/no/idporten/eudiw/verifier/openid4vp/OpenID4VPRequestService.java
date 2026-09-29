package no.idporten.eudiw.verifier.openid4vp;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
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
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.oauth2.sdk.id.State;
import jakarta.servlet.http.HttpSession;
import lombok.SneakyThrows;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.cache.CacheService;
import no.idporten.eudiw.verifier.config.ClientApplication;
import no.idporten.eudiw.verifier.config.VerifierServiceProperties;
import no.idporten.eudiw.verifier.crypto.ECUtils;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlQuery;
import no.idporten.lib.keystore.KeyProvider;
import no.idporten.lib.keystore.KeystoreManager;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.interfaces.ECPrivateKey;
import java.time.Clock;
import java.util.*;

@Service
public class OpenID4VPRequestService {

    private final VerifierServiceProperties verifierServiceProperties;
    private final KeystoreManager keystoreManager;
    private final VerificationTransactionService verificationTransactionService;
    private final JsonMapper jsonMapper;
    private final CacheService cacheService;

    public OpenID4VPRequestService(VerifierServiceProperties verifierServiceProperties, KeystoreManager keystoreManager, VerificationTransactionService verificationTransactionService, JsonMapper jsonMapper, CacheService cacheService) {
        this.verifierServiceProperties = verifierServiceProperties;
        this.keystoreManager = keystoreManager;
        this.verificationTransactionService = verificationTransactionService;
        this.jsonMapper = jsonMapper;
        this.cacheService = cacheService;
    }

    protected URI createRequestUri(ClientApplication clientApplication, String requestId, String flow) {
        return UriComponentsBuilder
                .fromUriString(verifierServiceProperties.getExternalBaseUri())
                .pathSegment("openid4vp", "authz-request", clientApplication.getId(),  requestId)
                .queryParam("flow", flow)
                .build()
                .toUri();
    }

    protected URI createResponseUri(ClientApplication clientApplication, String verifierTransactionId) {
        return UriComponentsBuilder
                .fromUriString(verifierServiceProperties.getExternalBaseUri())
                .pathSegment("openid4vp", "authz-response", clientApplication.getId(), verifierTransactionId)
                .build()
                .toUri();
    }

    protected URI createOpenID4VPAuthorizationRequest(String requestId, ClientApplication clientApplication, String flow) {
        return UriComponentsBuilder.newInstance()
                .scheme("eudi-openid4vp")
                .host(verifierServiceProperties.getSiop2ClientId())
                .queryParam("client_id", makeClientId(clientApplication))
                .queryParam("request_uri", createRequestUri(clientApplication, requestId, flow).toString())
                .build()
                .toUri();
    }

    protected String makeClientId(ClientApplication clientApplication)  {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new VerificationException("server_error", "Failed to generate client-id", e);
        }
        try {
            md.update(keystoreManager.getKeyProvider(clientApplication.getKeystoreName()).certificate().getEncoded());
        } catch (CertificateEncodingException e) {
            throw new VerificationException("server_error", "Failed to get keystore to generate client-id", e);
        }
        String clientId = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(md.digest());
        return "x509_hash:" + clientId;
    }

    public String createRequestId(ClientApplication clientApplication, String verifierTransactionId) {
        String requestId = UUID.randomUUID().toString();
        cacheService.putAuthorizationRequest(clientApplication, requestId, verifierTransactionId);
        return requestId;
    }

    @SneakyThrows
    public String retrieveAuthorizationRequest(HttpSession session, ClientApplication clientApplication, String requestId, String flow) {
        String verificationTransactionId = cacheService.retrieveAuthorizationRequest(clientApplication, requestId);
        if (verificationTransactionId == null) {
            throw new VerificationException("invalid_request", "Unknown authorization request");
        }
        VerificationTransaction verificationTransaction = verificationTransactionService.getVerificationTransaction(clientApplication, verificationTransactionId);
        if (verificationTransaction == null) {
            throw new VerificationException("invalid_request", "Unknown verification transaction");
        }
        String state = UUID.randomUUID().toString();
        JWK encryptionKey = new ECKeyGenerator(Curve.P_256).algorithm(JWEAlgorithm.ECDH_ES).keyUse(KeyUse.ENCRYPTION).keyIDFromThumbprint(true).generate();
        verificationTransaction.setState(state);
        verificationTransaction.setEncryptionKey(encryptionKey);
        verificationTransaction.setFlow(flow);
        JWT authorizationRequest = makeRequestJwt(session, verificationTransaction, clientApplication, verificationTransactionId);
        verificationTransaction.setRequest(authorizationRequest.getJWTClaimsSet().toJSONObject());
        cacheService.updateVerificationTransaction(clientApplication, verificationTransactionId, verificationTransaction);
        return authorizationRequest.serialize();
    }

    public URI createAuthorizationRequest(String requestId, ClientApplication clientApplication, String flow) {
        return createOpenID4VPAuthorizationRequest(requestId, clientApplication, flow);
    }

    public URI createQrCodeDataURI(URI crossDeviceAuthorizationRequest) throws Exception {
        QRCodeWriter barcodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = barcodeWriter.encode(crossDeviceAuthorizationRequest.toString(), BarcodeFormat.QR_CODE, 200, 200);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            byte[] qrBytes = outputStream.toByteArray();
            return URI.create("data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(qrBytes));
        }
    }

    private JWT makeRequestJwt(HttpSession session, VerificationTransaction verificationTransaction, ClientApplication clientApplication, String verificationTransactionId) throws Exception {
        KeyProvider keyProvider = keystoreManager.getKeyProvider(verificationTransaction.getClientApplication().getKeystoreName());
        List<Base64> certChain = new ArrayList<>();
        certChain.add(Base64.encode(keyProvider.certificate().getEncoded()));
        SessionRecordElements sessionRecordElements = new SessionRecordElements(new Nonce(),new Audience("https://self-issued.me/v2"));
        session.setAttribute("sessionRecordElements", sessionRecordElements);

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .audience(sessionRecordElements.aud().getValue())
                .issuer(verifierServiceProperties.getExternalBaseUri())
                .claim("response_uri", createResponseUri(clientApplication, verificationTransactionId).toString())
                .claim("response_type", "vp_token")
                .claim("response_mode", "direct_post.jwt")
                .claim("nonce", sessionRecordElements.nonce().getValue())
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
