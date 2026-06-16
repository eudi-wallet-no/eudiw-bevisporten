package no.idporten.eudiw.login.openid4vp;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.factories.DefaultJWEDecrypterFactory;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ResponseMode;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.client.ClientMetadata;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientMetadata;
import id.walt.mdoc.dataelement.DataElement;
import id.walt.mdoc.dataelement.EncodedCBORElement;
import id.walt.mdoc.dataelement.MapElement;
import id.walt.mdoc.dataelement.MapKey;
import id.walt.mdoc.dataretrieval.DeviceResponse;
import id.walt.mdoc.doc.MDoc;
import id.walt.mdoc.issuersigned.IssuerSigned;
import lombok.SneakyThrows;
import no.idporten.eudiw.login.openid4vp.protocol.*;
import no.idporten.lib.keystore.KeyProvider;
import no.idporten.lib.keystore.KeystoreConfig;
import no.idporten.lib.keystore.KeystoreManager;
import no.idporten.lib.keystore.spring.KeystoreConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.interfaces.ECPrivateKey;
import java.text.ParseException;
import java.time.Clock;
import java.util.*;

/**
 * Service creating and verifying OpenID4VP requests and responses.
 */
@Service
public class OpenID4VPService {

    public static final String OPENID4VP_ACCESS_KEYSTORE_NAME = "openid4vp";
    private final OpenID4VPProperties openID4VPProperties;
    private final KeystoreManager keystoreManager;
    private final KeystoreConfigurationProperties keystoreConfigurationProperties;

    public static final String JWT_TYPE_OAUTH_AUTHZ_REQ = "oauth-authz-req+jwt";

    public OpenID4VPService(OpenID4VPProperties openID4VPProperties, KeystoreManager keystoreManager, KeystoreConfigurationProperties keystoreConfigurationProperties) {
        this.openID4VPProperties = openID4VPProperties;
        this.keystoreManager = keystoreManager;
        this.keystoreConfigurationProperties = keystoreConfigurationProperties;
    }

    @SneakyThrows
    private String x509HashClientId(Certificate certificate) {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(certificate.getEncoded());
        String clientId = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(md.digest());
        return "x509_hash:" + clientId;
    }

    public AuthorizationRequest createAuthorizationRequest(OpenID4VPFlow flow, String state) {
        URI requestUri = UriComponentsBuilder.fromUriString(openID4VPProperties.getBaseUri()).pathSegment("openid4vp", "request", flow.name().toLowerCase(), state).build().toUri();
        URI authorizeEndpoint = URI.create("eudi-openid4vp://" + openID4VPProperties.getSiop2ClientId());
        return new AuthorizationRequest.Builder(
                requestUri,
                new ClientID(x509HashClientId(keystoreManager.getKeyProvider(OPENID4VP_ACCESS_KEYSTORE_NAME).certificate())))
                .endpointURI(authorizeEndpoint)
                .build();
    }

    public SignedJWT createPresentationRequest(OpenID4VPFlow flow, String state) throws JOSEException, CertificateEncodingException {
        URI responseUri = UriComponentsBuilder.fromUriString(openID4VPProperties.getBaseUri()).pathSegment("openid4vp", "response", flow.name().toLowerCase(), state).build().toUri();
        KeyProvider keyProvider = keystoreManager.getKeyProvider(OPENID4VP_ACCESS_KEYSTORE_NAME);
        AuthorizationRequest authorizationRequest =
                new AuthorizationRequest.Builder(
                        new ResponseType("vp_token"),
                        new ClientID(x509HashClientId(keyProvider.certificate())))
                        .responseMode(new ResponseMode("direct_post.jwt"))
                        .state(new State(state))
                        .customParameter("nonce", new Nonce().getValue())
                        .customParameter("response_uri", responseUri.toString())
                        .build();
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder(authorizationRequest.toJWTClaimsSet());
        builder
                .claim("dcql_query", makeDCQLQuery(openID4VPProperties.getCredentialConfig()))
                .claim("client_metadata", makeClientMetadata().toJSONObject())
                .audience("https://self-issued.me/v2")
                .issuer(openID4VPProperties.getSiop2ClientId())
                .jwtID(UUID.randomUUID().toString())
                .issueTime(new Date(Clock.systemUTC().millis()))
                .expirationTime(new Date(Clock.systemUTC().millis() + 120000));
        JWTClaimsSet claims = builder.build();
        JWSSigner signer = new ECDSASigner((ECPrivateKey) keyProvider.privateKey());
        SignedJWT jar = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.ES256)
                        .x509CertChain(List.of(Base64.encode(keyProvider.certificate().getEncoded())))
                        .type(new JOSEObjectType(JWT_TYPE_OAUTH_AUTHZ_REQ))
                        .build(),
                claims);
        jar.sign(signer);
        return jar;
    }

    public DCQLQuery makeDCQLQuery(CredentialConfig credentialConfig) {
        return DCQLQuery.builder()
                .credential(
                        Credential.builder()
                                .id("login")
                                .format(credentialConfig.getFormat())
                                .meta(Meta.builder().doctypeValue(credentialConfig.getDocType()).build())
                                .claim(Claim.builder()
                                        .path(credentialConfig.getPath())
                                        .build())
                                .build()).build();
    }

    // TODO her er nækkelhåndteringen generelt helt dum, må bruke flyktige nøækler og ikke fra access keystore!! - lager egen sak på det og da fjernes sneakythrows også for dette hacket
    @SneakyThrows
    public ClientMetadata makeClientMetadata() {
        OIDCClientMetadata metadata = new OIDCClientMetadata();
        // TODO burde bruke flyktige nøkler her - her hardkodes et alias og password for å tweake rundt
        KeyStore keyStore = keystoreManager.getKeystore(OPENID4VP_ACCESS_KEYSTORE_NAME);
        KeystoreConfig keystoreConfig = keystoreConfigurationProperties.getKeystore(OPENID4VP_ACCESS_KEYSTORE_NAME);
        ECKey ecKey = new ECKey.Builder(ECKey.load(keyStore,keystoreConfig.keyAlias(), keystoreConfig.keyPassword().toCharArray())).keyUse(KeyUse.ENCRYPTION).algorithm(JWEAlgorithm.ECDH_ES).build();
        metadata.setJWKSet(new JWKSet(List.of(ecKey)).toPublicJWKSet());
        metadata.setCustomField("encrypted_response_enc_values_supported",
                List.of(
                        EncryptionMethod.A128GCM.getName(),
                        EncryptionMethod.A256GCM.getName()));
        metadata.setCustomField("vp_formats_supported", VpFormat
                .builder()
                .claimFormat("mso_mdoc", MdocFormatParameters.builder().build())
                .claimFormat("dc+sd-jwt", SDJwtVCFormatParameters.builder()
                        .sdJwtAlgValues(List.of(JWSAlgorithm.ES256, JWSAlgorithm.ES384))
                        .kbJwtAuthAlgValues(List.of(JWSAlgorithm.ES256, JWSAlgorithm.ES384))
                        .build())
                .build());
        return metadata;
    }

    public URI createRedirectURI(OpenID4VPFlow flow, String state) {
        return UriComponentsBuilder.fromUriString(openID4VPProperties.getBaseUri()).pathSegment("login", state).build().toUri();
    }

    public Map<String, String> handleWalletResponse(String response) throws Exception {
        Map<String, Object> claimsFromJwePayload = decryptAndDeserializeJweResponse(response);
        String state = (String) claimsFromJwePayload.get("state");
        Map<String, List<String>> vpToken = (Map<String, List<String>>) claimsFromJwePayload.get("vp_token");
        List<String> loginTokens = vpToken.get("login");
        return retrieveElementsFromPidDocumentInMDoc(loginTokens.getFirst());
    }

    private Map<String, Object> decryptAndDeserializeJweResponse(String response) throws ParseException, JOSEException {
        KeyProvider keyProvider = keystoreManager.getKeyProvider("openid4vp");
        JWEObject jwe = JWEObject.parse(response);
        JWEDecrypter decrypter = new DefaultJWEDecrypterFactory().createJWEDecrypter(jwe.getHeader(), keyProvider.privateKey());
        jwe.decrypt(decrypter);
        return jwe.getPayload().toJSONObject();
    }

    private Map<String, String> retrieveElementsFromPidDocumentInMDoc(String vpToken) {
        DeviceResponse deviceResponse = DeviceResponse.Companion.fromCBORBase64URL(vpToken);
        Map<String, String> claims = new HashMap<>();
        for (MDoc mDoc : deviceResponse.getDocuments()) {
            mDoc.getMSO(); // TODO verify med hvilke nøkler?
            mDoc.verifyDocType();
            mDoc.verifyIssuerSignedItems();
            mDoc.verifyValidity();
            IssuerSigned issuerSigned = mDoc.getIssuerSigned();
            for (String namespace : issuerSigned.getNameSpaces().keySet()) {
                List<EncodedCBORElement> elements = issuerSigned.getNameSpaces().get(namespace);
                for (EncodedCBORElement element : elements) {
                    Map<MapKey, DataElement> elementMap = ((MapElement) element.decode()).getValue();
                    String elementIdentifier = null;
                    String elementValue = null;
                    for (MapKey mapKey : elementMap.keySet()) {
                        if (mapKey.getStr().equals("elementIdentifier")) {
                            elementIdentifier = String.valueOf(elementMap.get(mapKey).getInternalValue());
                        }
                        if (mapKey.getStr().equals("elementValue")) {
                            elementValue = String.valueOf(elementMap.get(mapKey).getInternalValue());
                        }
                    }
                    claims.put(elementIdentifier, elementValue);
                }
            }
        }
        return claims;
    }

}
