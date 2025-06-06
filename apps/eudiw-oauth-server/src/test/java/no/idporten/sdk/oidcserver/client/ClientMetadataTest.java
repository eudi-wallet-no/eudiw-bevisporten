package no.idporten.sdk.oidcserver.client;

import no.idporten.sdk.oidcserver.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When loading and validating client metadata")
public class ClientMetadataTest {

    @Test
    @DisplayName("then a client must have a client_id")
    public void testClientMustHaveAnId() {
        ClientMetadata clientMetadata = TestUtils.defaultClientMetedataBuilder().clientId(null).build();
        try {
            clientMetadata.validate();
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("must have a client id"));
        }
    }

    @Test
    @DisplayName("then a client must have a client_secret")
    public void testClientMustHaveASecret() {
        ClientMetadata clientMetadata = TestUtils.defaultClientMetedataBuilder().clientSecret(null).build();
        try {
            clientMetadata.validate();
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("does not have a client secret"));
        }
    }

    @Test
    @DisplayName("then a client must have at least one redirect_uri in redirect_uris")
    public void testClientMustHaveARedirectUri() {
        ClientMetadata clientMetadata = TestUtils.defaultClientMetedataBuilder().build();
        clientMetadata.setRedirectUris(Collections.emptyList());
        try {
            clientMetadata.validate();
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("does not have any redirect uris"));
        }
    }

    @Test
    @DisplayName("then a client must support the openid scope")
    public void testClientMustSupportTheOpenidScope() {
        ClientMetadata clientMetadata = TestUtils.defaultClientMetedataBuilder().build();
        clientMetadata.setScopes(Collections.singletonList("profile"));
        try {
            clientMetadata.validate();
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("does not support the openid scope"));
        }
    }

    @Test
    @DisplayName("then the default client for junit tests is OK")
    public void testValidateDefaultClient() {
        TestUtils.defaultClientMetadata().validate();
    }

    @Test
    @DisplayName("then a redirect_uri must have a value")
    public void testRedirectUriMustHaveValue() {
        try {
            ClientMetadata.builder().build().validateRedirectUri("c", null);
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("has an empty redirect_uri"));
        }
    }

    @Test
    @DisplayName("then a redirect_uri must be a URI")
    public void testRedirectUriMustBeAUri() {
        try {
            ClientMetadata.builder().build().validateRedirectUri("c", "fo\\//o");
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("redirect_uri with invalid format"));
        }
    }

    @Test
    @DisplayName("then a redirect_uri must use scheme http or https")
    public void testRedirectUriMustBeHttpOrHttps() {
        ClientMetadata clientMetadata = ClientMetadata.builder().build();
        clientMetadata.validateRedirectUri("c", "http://localhost:1234");
        clientMetadata.validateRedirectUri("c", "https://digdir.no");
        try {
            clientMetadata.validateRedirectUri("c", "gopher://digdir.no");
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("redirect_uri that is not http(s)"));
        }
    }

    @Test
    @DisplayName("then a redirect_uri cannot use http if not localhost or docker uri (http://someservice:1234)")
    public void testHttpRedirectUriOnlyAllowedForLocalhost() {
        ClientMetadata clientMetadata = ClientMetadata.builder().build();
        clientMetadata.validateRedirectUri("c", "http://localhost:1234");
        clientMetadata.validateRedirectUri("c", "http://127.0.0.1:1234");
        clientMetadata.validateRedirectUri("c", "http://dockersomething:1234");
        try {
            clientMetadata.validateRedirectUri("c", "http://digdir.no");
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("redirect_uri that is http and not localhost"));
        }
    }

    @Test
    @DisplayName("then a redirect_uri can not use URI fragments")
    public void testRedirectUriCannotHaveFragment() {
        try {
            ClientMetadata.builder().build().validateRedirectUri("c", "https://digdir.no/idporten#foo");
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("redirect_uri that has a fragment"));
        }
    }

    @Test
    @DisplayName("then a client can have optional metadata (client_name, logo_uri)")
    public void testOptionalMetadata() {
        ClientMetadata clientMetadata = TestUtils.defaultClientMetedataBuilder()
                .clientName("myclient")
                .logoUri("https://junit.digdir.no/logo.png")
                .build();
        assertAll(
                () -> assertEquals("myclient", clientMetadata.getClientName()),
                () -> assertEquals("https://junit.digdir.no/logo.png", clientMetadata.getLogoUri()),
                () -> assertNotNull(clientMetadata.getFeatures()),
                () -> assertTrue(clientMetadata.getFeatures().isEmpty())
        );
    }

    @Test
    @DisplayName("then a client does not need to have optional metadata")
    public void testOptionalMetadataIsOptional() {
        ClientMetadata clientMetadata = TestUtils.defaultClientMetedataBuilder()
                .build();
        assertAll(
                () -> assertNull(clientMetadata.getClientName()),
                () -> assertNull(clientMetadata.getLogoUri()),
                () -> assertNotNull(clientMetadata.getFeatures()),
                () -> assertTrue(clientMetadata.getFeatures().isEmpty())
        );
    }


    @Test
    @DisplayName("then a client can have optional features")
    public void testOptionalFeatures() {
        ClientMetadata clientMetadata = TestUtils.defaultClientMetedataBuilder()
                .feature("is_xxx", true)
                .feature("something", "special")
                .build();
        assertAll(
                () -> assertNotNull(clientMetadata.getFeatures()),
                () -> assertEquals(2, clientMetadata.getFeatures().size()),
                () -> assertEquals(true, clientMetadata.getFeatures().get("is_xxx")),
                () -> assertEquals("special", clientMetadata.getFeatures().get("something"))
        );
    }

    @Test
    @DisplayName("then a client's optional logo_uri must have correct uri format")
    public void tetsLogoUriMustBeValidUris() {
        try {
            ClientMetadata.builder().build().validateLogoUri("myclient", "fo\\//o");
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("myclient has logo_uri with invalid format"));
        }
    }

    @Test
    @DisplayName("then a client's optional logo_uri must use http or https")
    public void tetsLogoUriMustUseHttpOrHttpsScheme() {
        try {
            ClientMetadata.builder().build().validateLogoUri("myclient", "ftp://logoserver/logo.jpg");
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("myclient has logo_uri that is not http(s)"));
        }
    }


}
