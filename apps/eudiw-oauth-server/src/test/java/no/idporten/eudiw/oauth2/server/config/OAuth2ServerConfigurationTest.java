package no.idporten.eudiw.oauth2.server.config;

import no.idporten.eudiw.oauth2.server.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URI;
import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When setting up the OAuth2 authorization server")
public class OAuth2ServerConfigurationTest {


    private KeyStore loadTestKeyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("junit.jks")) {
            keyStore.load(is, "secret".toCharArray());
        }
        return keyStore;
    }

    @Test
    @DisplayName("then a key pair from a JKS keystore can be converted to a JWK")
    public void testConvertKeystoreFromJksToJwk() throws Exception {
        KeyStore keyStore = loadTestKeyStore();
        OAuth2ServerConfiguration serverConfiguration = TestUtils.defaultOAuth2ServerTestConfigurationBuilder()
                .keystore(keyStore, "junit", "secret")
                .build();
        assertNotNull(serverConfiguration.getJwk());
        serverConfiguration.validate();
    }

    @Test
    @DisplayName("then response_mode = query is added to config by default")
    public void testAlwaysAddQueryToResponseMode() throws Exception {
        OAuth2ServerConfiguration serverConfiguration = TestUtils.defaultOAuth2ServerTestConfigurationBuilder()
                .responseMode("form_post")
                .build();
        assertTrue(serverConfiguration.getResponseModes().contains("query"));
        assertTrue(serverConfiguration.getResponseModes().contains("form_post"));
    }

    @Test
    @DisplayName("then response_mode = query, form_post and query.jwt are supported")
    public void testSupportedResponseModes() throws Exception {
        OAuth2ServerConfiguration serverConfiguration = TestUtils.defaultOAuth2ServerTestConfigurationBuilder()
                .responseMode("query.jwt")
                .responseMode("form_post")
                .build();
        assertAll(
                () -> assertTrue(serverConfiguration.getResponseModes().contains("query")),
                () -> assertTrue(serverConfiguration.getResponseModes().contains("query.jwt")),
                () -> assertTrue(serverConfiguration.getResponseModes().contains("form_post"))
        );
    }

    @Test
    @DisplayName("then an illegal response_mode is not allowed")
    void testDetectIllegalResponseMode() throws Exception {
        OAuth2ServerConfiguration serverConfiguration = TestUtils.defaultOAuth2ServerTestConfigurationBuilder()
                .responseMode("fårm_påst")
                .build();
        try {
            serverConfiguration.validate();
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("illegal values in list of values for responseModes"));
        }
    }

    @Test
    @DisplayName("then supported authorization_details types are recognized")
    void testSupportAuthorizationDetailsTypes() throws Exception {
        OAuth2ServerConfiguration serverConfiguration = TestUtils.defaultOAuth2ServerTestConfigurationBuilder()
                .authorizationDetailsTypeSupported("t")
                .build();
        assertAll(
                () -> assertTrue(serverConfiguration.supportsAuthorizationDetailsType("t")),
                () -> assertFalse(serverConfiguration.supportsAuthorizationDetailsType("T"))
        );
    }


    @Test
    @DisplayName("then only http and https schemes are allowed for OAuth2/OIDC endpoint URIs")
    void testOnlyHttpAndHttpsEndpointUrisAllowed() throws Exception {
        OAuth2ServerConfiguration serverConfiguration = TestUtils.defaultEmbeddedSercerTestConfiguration();
        serverConfiguration.validateUri("http", true, new URI("http://localhost/"));
        serverConfiguration.validateUri("https", true, new URI("https://digdir.no/"));
        try {
            serverConfiguration.validateUri("javascript", true, new URI("javascript://digdir.no/"));
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("requires a http(s) uri"));
        }
    }

    @Test
    @DisplayName("then fragments are not allowed in OAuth2/OIDC endpoint URIs")
    void testFragmentsNotAllowedInEndpointUris() throws Exception {
        OAuth2ServerConfiguration serverConfiguration = TestUtils.defaultEmbeddedSercerTestConfiguration();
        serverConfiguration.validateUri("x", true, new URI("https://digdir.no/"));
        try {
            serverConfiguration.validateUri("x", true, new URI("https://digdir.no/foo#bar"));
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("requires an uri without fragment"));
        }
    }

    @Test
    @DisplayName("then cache object lifetimes must be greater than zero seconds")
    void testLifetimesMustBePositiveAboveZero() throws Exception {
        OAuth2ServerConfiguration serverConfiguration = TestUtils.defaultEmbeddedSercerTestConfiguration();
        serverConfiguration.validateLifetime("x", 1);
        try {
            serverConfiguration.validateLifetime("x", 0);
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("requires a positive lifetime"));
        }
    }

    @Test
    @DisplayName("then the discovery endpoint uri is calculated from the issuer uri")
    void testCalculateDiscoveryEndpointUriFromIssuer() throws Exception {
        OAuth2ServerConfiguration serverConfiguration = TestUtils.defaultEmbeddedSercerTestConfiguration();
        assertEquals("http://my-test-server/.well-known/openid-configuration", serverConfiguration.getOidcDiscoveryEndpoint().toString());
        serverConfiguration = TestUtils.defaultOAuth2ServerTestConfigurationBuilder().issuer(URI.create("https://junit.idporten.no/foo")).build();
        assertEquals("https://junit.idporten.no/foo/.well-known/openid-configuration", serverConfiguration.getOidcDiscoveryEndpoint().toString());
    }

    @Test
    @DisplayName("then the iss authorization response parameter is supported by default")
    void testSupportIssParameterDefaultTrue() throws Exception {
        OAuth2ServerConfiguration serverConfiguration = TestUtils.defaultEmbeddedSercerTestConfiguration();
        assertTrue(serverConfiguration.isAuthorizationResponseIssParameterSupported());
    }

}
