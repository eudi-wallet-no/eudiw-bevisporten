package no.idporten.sdk.oidcserver;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.dpop.DPoPProofFactory;
import com.nimbusds.oauth2.sdk.dpop.DefaultDPoPProofFactory;
import no.idporten.sdk.oidcserver.client.ClientMetadata;
import no.idporten.sdk.oidcserver.config.OpenIDConnectSdkConfiguration;

import java.net.URI;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.*;

/**
 * Utilities for writing tests for the SDK.
 */
public class TestUtils {

    /**
     * Create a String for HTTP basic authentication.
     * @param clientId client id
     * @param clientSecret client secret
     * @return Basic auth header value
     */
    public static String basicAuthHeader(String clientId, String clientSecret) {
        return "Basic " + new String(Base64
                .getEncoder()
                .withoutPadding()
                .encode("%s:%s".formatted(clientId, clientSecret).getBytes(Charset.defaultCharset())));
    }

    private final static String defaultIssuer = "http://my-test-server";

    /**
     * A default client for testing.
     */
    public static ClientMetadata defaultClientMetadata() {
        return defaultClientMetadataBuilder().build();
    }

    /**
     * A builder creating a default client for testing.
     */
    public static ClientMetadata.ClientMetadataBuilder defaultClientMetadataBuilder() {
        return ClientMetadata.builder()
                .clientId("cid")
                .clientSecret("86258f7f-4be6-4b4a-9391-1123ee1b567a")
                .redirectUri("https://idporten.no/")
                .redirectUri("https://2.idporten.no/")
                .scope("openid");
    }

    /**
     * A default issuer for testing.
     */
    public static String defaultIssuer() {
        return defaultIssuer;
    }

    /**
     * A default SDK configuration for testing.
     */
    public static OpenIDConnectSdkConfiguration defaultSdkTestConfiguration() throws Exception {
        return defaultSdkTestConfigurationBuilder().build();
    }

    /**
     * A builder for default SDK configuration for testing.
     */
    public static OpenIDConnectSdkConfiguration.OpenIDConnectSdkConfigurationBuilder defaultSdkTestConfigurationBuilder() throws Exception {
        return OpenIDConnectSdkConfiguration.builder()
                .issuer(new URI(defaultIssuer()))
                .grantTypesSupported(List.of("urn:ietf:params:oauth:grant-type:pre-authorized_code", "authorization_code"))
                .cache(new SimpleOpenIDConnectCache())
                .jwk(new ECKeyGenerator(Curve.P_256)
                        .keyUse(KeyUse.SIGNATURE)
                        .keyID("test-kid")
                        .generate())
                .client(defaultClientMetadata())
                .acrValue("Level3")
                .acrValue("Level4")
                .uiLocale("nn");
    }

    public static SignedJWT createClientSecretJWT(ClientMetadata clientMetadata, String... audience) throws Exception {
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256),
                new JWTClaimsSet.Builder()
                        .issuer(clientMetadata.getClientId())
                        .audience(audience == null ? null : Arrays.asList(audience))
                        .subject(clientMetadata.getClientId())
                        .jwtID(UUID.randomUUID().toString())
                        .expirationTime(new Date(new Date().getTime() + 1000 * 60 * 60 * 24))
                        .build());
        signedJWT.sign(new MACSigner(clientMetadata.getClientSecret()));
        return signedJWT;
    }

    public static String createDpopHeader(URI tokenEndpoint) throws JOSEException {
        DPoPProofFactory proofFactory = getDPoPProofFactory();
        SignedJWT dPopProof = proofFactory.createDPoPJWT("POST", tokenEndpoint);
        return "%s.%s.%s".formatted(dPopProof.getHeader().toBase64URL(), dPopProof.getPayload().toBase64URL(), dPopProof.getSignature());
    }

    public static String createDpopHeader(URI tokenEndpoint, DPoPProofFactory dPoPProofFactory) throws JOSEException {
        SignedJWT dPopProof = dPoPProofFactory.createDPoPJWT("POST", tokenEndpoint);
        return "%s.%s.%s".formatted(dPopProof.getHeader().toBase64URL(), dPopProof.getPayload().toBase64URL(), dPopProof.getSignature());
    }

    public static DPoPProofFactory getDPoPProofFactory() throws JOSEException {
        ECKey clientJWK = new ECKeyGenerator(Curve.P_256).keyIDFromThumbprint(true).generate();
        JWSAlgorithm jwsAlg = JWSAlgorithm.ES256;
        return new DefaultDPoPProofFactory(clientJWK, jwsAlg);
    }


    public static String createDpopJtk() throws JOSEException {
        ECKey clientJWK = new ECKeyGenerator(Curve.P_256).keyIDFromThumbprint(true).generate();
        return clientJWK.computeThumbprint().toString();
    }

    public static String findDpopJktFromDpopHeader(String dPoPHeader) throws JOSEException, ParseException {
        SignedJWT dPopProof = SignedJWT.parse(dPoPHeader);
        JWK jwk = dPopProof.getHeader().getJWK();
        Base64URL thumbprint = jwk.toPublicJWK().computeThumbprint();
        return thumbprint.toString();
    }

}



