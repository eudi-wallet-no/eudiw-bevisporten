package no.idporten.sdk.oidcserver.config;

import no.idporten.sdk.oidcserver.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URI;
import java.security.KeyStore;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When setting up the OpenID Connect SDK")
public class OpenIDConnectSDKConfigurationTest {


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
        OpenIDConnectSdkConfiguration sdkConfiguration = TestUtils.defaultSdkTestConfigurationBuilder()
                .keystore(keyStore, "junit", "secret")
                .build();
        assertNotNull(sdkConfiguration.getJwk());
        sdkConfiguration.validate();
    }

    @Test
    @DisplayName("then response_mode = query is added to config by default")
    public void testAlwaysAddQueryToResponseMode() throws Exception {
        OpenIDConnectSdkConfiguration sdkConfiguration = TestUtils.defaultSdkTestConfigurationBuilder()
                .responseMode("form_post")
                .build();
        assertTrue(sdkConfiguration.getResponseModes().contains("query"));
        assertTrue(sdkConfiguration.getResponseModes().contains("form_post"));
    }

    @Test
    @DisplayName("then response_mode = query, form_post and query.jwt are supported")
    public void testSupportedResponseModes() throws Exception {
        OpenIDConnectSdkConfiguration sdkConfiguration = TestUtils.defaultSdkTestConfigurationBuilder()
                .responseMode("query.jwt")
                .responseMode("form_post")
                .build();
        assertAll(
                () -> assertTrue(sdkConfiguration.getResponseModes().contains("query")),
                () -> assertTrue(sdkConfiguration.getResponseModes().contains("query.jwt")),
                () -> assertTrue(sdkConfiguration.getResponseModes().contains("form_post"))
        );
    }

    @Test
    @DisplayName("then the openid scope is supported by default")
    void testAlwaysAddOpenidToScopesSupported() throws Exception {
        OpenIDConnectSdkConfiguration sdkConfiguration = TestUtils.defaultSdkTestConfigurationBuilder()
                .scopeSupported("prefix:customscope")
                .build();
        assertTrue(sdkConfiguration.supportsScope("openid"));
        assertTrue(sdkConfiguration.supportsScope("prefix:customscope"));
    }

    @Test
    @DisplayName("then empty claims in claims supported is not allowed")
    void testDetectEmptyClaimsInClaimsSupported() throws Exception {
        OpenIDConnectSdkConfiguration sdkConfiguration = TestUtils.defaultSdkTestConfigurationBuilder()
                .claimSupported("claim")
                .claimSupported("")
                .build();
        try {
            sdkConfiguration.validate();
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("requires a list of non-empty values for claimsSupported"));
        }
    }

    @Test
    @DisplayName("then an illegal response_mode is not allowed")
    void testDetectIllegalResponseMode() throws Exception {
        OpenIDConnectSdkConfiguration sdkConfiguration = TestUtils.defaultSdkTestConfigurationBuilder()
                .responseMode("fårm_påst")
                .build();
        try {
            sdkConfiguration.validate();
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("illegal values in list of values for responseModes"));
        }
    }

    @Test
    @DisplayName("then supported claims are recognized")
    void testSupportClaims() throws Exception {
        OpenIDConnectSdkConfiguration sdkConfiguration = TestUtils.defaultSdkTestConfigurationBuilder()
                .claimSupported("c")
                .build();
        assertAll(
                () -> assertTrue(sdkConfiguration.supportsClaim("c")),
                () -> assertFalse(sdkConfiguration.supportsClaim("d"))
        );
    }

    @Test
    @DisplayName("then supported authorization_details types are recognized")
    void testSupportAuthorizationDetailsTypes() throws Exception {
        OpenIDConnectSdkConfiguration sdkConfiguration = TestUtils.defaultSdkTestConfigurationBuilder()
                .authorizationDetailsTypeSupported("t")
                .build();
        assertAll(
                () -> assertTrue(sdkConfiguration.supportsAuthorizationDetailsType("t")),
                () -> assertFalse(sdkConfiguration.supportsAuthorizationDetailsType("T"))
        );
    }


    @Test
    @DisplayName("then only http and https schemes are allowed for OAuth2/OIDC endpoint URIs")
    void testOnlyHttpAndHttpsEndpointUrisAllowed() throws Exception {
        OpenIDConnectSdkConfiguration sdkConfiguration = TestUtils.defaultSdkTestConfiguration();
        sdkConfiguration.validateUri("http", true, new URI("http://localhost/"));
        sdkConfiguration.validateUri("https", true, new URI("https://digdir.no/"));
        try {
            sdkConfiguration.validateUri("javascript", true, new URI("javascript://digdir.no/"));
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("requires a http(s) uri"));
        }
    }

    @Test
    @DisplayName("then fragments are not allowed in OAuth2/OIDC endpoint URIs")
    void testFragmentsNotAllowedInEndpointUris() throws Exception {
        OpenIDConnectSdkConfiguration sdkConfiguration = TestUtils.defaultSdkTestConfiguration();
        sdkConfiguration.validateUri("x", true, new URI("https://digdir.no/"));
        try {
            sdkConfiguration.validateUri("x", true, new URI("https://digdir.no/foo#bar"));
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("requires an uri without fragment"));
        }
    }

    @Test
    @DisplayName("then cache object lifetimes must be greater than zero seconds")
    void testLifetimesMustBePositiveAboveZero() throws Exception {
        OpenIDConnectSdkConfiguration sdkConfiguration = TestUtils.defaultSdkTestConfiguration();
        sdkConfiguration.validateLifetime("x", 1);
        try {
            sdkConfiguration.validateLifetime("x", 0);
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("requires a positive lifetime"));
        }
    }

    @Test
    @DisplayName("then the discovery endpoint uri is calculated from the issuer uri")
    void testCalculateDiscoveryEndpointUriFromIssuer() throws Exception {
        OpenIDConnectSdkConfiguration sdkConfiguration = TestUtils.defaultSdkTestConfiguration();
        assertEquals("http://junittest.idporten.no/.well-known/openid-configuration", sdkConfiguration.getOidcDiscoveryEndpoint().toString());
        sdkConfiguration = TestUtils.defaultSdkTestConfigurationBuilder().issuer(URI.create("https://junit.idporten.no/foo")).build();
        assertEquals("https://junit.idporten.no/foo/.well-known/openid-configuration", sdkConfiguration.getOidcDiscoveryEndpoint().toString());
    }

    @Test
    @DisplayName("then the iss authorization response parameter is supported by default")
    void testSupportIssParameterDefaultTrue() throws Exception {
        OpenIDConnectSdkConfiguration sdkConfiguration = TestUtils.defaultSdkTestConfiguration();
        assertTrue(sdkConfiguration.isAuthorizationResponseIssParameterSupported());
    }

}
