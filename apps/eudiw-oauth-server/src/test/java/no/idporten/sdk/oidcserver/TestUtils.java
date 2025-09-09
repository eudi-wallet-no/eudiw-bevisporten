package no.idporten.sdk.oidcserver;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.sdk.oidcserver.client.ClientMetadata;
import no.idporten.sdk.oidcserver.config.OpenIDConnectSdkConfiguration;

import java.net.URI;
import java.nio.charset.Charset;
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

    private static String defaultIssuer = "http://junittest.idporten.no/";

    /**
     * A default client for testing.
     */
    public static ClientMetadata defaultClientMetadata() {
        return defaultClientMetedataBuilder().build();
    }

    /**
     * A builder creating a default client for testing.
     */
    public static ClientMetadata.ClientMetadataBuilder defaultClientMetedataBuilder() {
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

}



