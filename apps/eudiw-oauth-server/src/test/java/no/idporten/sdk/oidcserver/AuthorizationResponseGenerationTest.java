package no.idporten.sdk.oidcserver;

import no.idporten.sdk.oidcserver.client.ClientMetadata;
import no.idporten.sdk.oidcserver.config.OpenIDConnectSdkConfiguration;
import no.idporten.sdk.oidcserver.protocol.Authorization;
import no.idporten.sdk.oidcserver.protocol.AuthorizationResponse;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthorizationResponseGenerationTest {

    OpenIDConnectIntegration initSdk(boolean authorizationResponseIssParameterSupported) throws Exception {
        ClientMetadata client1 = ClientMetadata.builder().clientId("client1").clientSecret("secret").scope("openid").redirectUri("https://junit.idporten.no/").build();
        OpenIDConnectSdkConfiguration sdkConfiguration = TestUtils.defaultSdkTestConfigurationBuilder()
                .client(client1)
                .authorizationDetailsTypeSupported("foo")
                .authorizationResponseIssParameterSupported(authorizationResponseIssParameterSupported)
                .build();
        return new OpenIDConnectIntegrationBase(sdkConfiguration);
    }

    @DisplayName("When the iss parameter is supported")
    @Nested
    class IssParameterSupportedTests {

        @Test
        @DisplayName("then an authorization response should include the iss parameter")
        public void testAuthorize() throws Exception {
            OpenIDConnectIntegration openIDConnectSdk = initSdk(true);
            MockRequest request = new MockRequest();
            request.addParameter("client_id", "c");
            PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
            Authorization authorization = Authorization.builder().sub("p").build();
            AuthorizationResponse authorizationResponse = openIDConnectSdk.authorize(pushedAuthorizationRequest, authorization);
            assertAll(
                    () -> assertEquals(openIDConnectSdk.getSDKConfiguration().getIssuer().toString(), authorizationResponse.toResponseParameters().get("iss")),
                    () -> assertEquals(openIDConnectSdk.getSDKConfiguration().getIssuer().toString(), authorizationResponse.getIss()),
                    () -> assertNotNull(authorizationResponse.getCode()),
                    () -> assertEquals(authorizationResponse.getCode(), authorizationResponse.toResponseParameters().get("code")),
                    () -> assertFalse(authorizationResponse.toResponseParameters().containsKey("error"))
            );
        }

        @Test
        @DisplayName("then an error authorization response should include the iss parameter")
        public void testErrorResponse() throws Exception {
            OpenIDConnectIntegration openIDConnectSdk = initSdk(true);
            MockRequest request = new MockRequest();
            PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
            AuthorizationResponse authorizationResponse = openIDConnectSdk.errorResponse(pushedAuthorizationRequest, "foo", "msg");
            assertAll(
                    () -> assertEquals(openIDConnectSdk.getSDKConfiguration().getIssuer().toString(), authorizationResponse.toResponseParameters().get("iss")),
                    () -> assertEquals(openIDConnectSdk.getSDKConfiguration().getIssuer().toString(), authorizationResponse.getIss()),
                    () -> assertNull(authorizationResponse.getCode()),
                    () -> assertEquals("foo", authorizationResponse.toResponseParameters().get("error")),
                    () -> assertEquals("msg", authorizationResponse.toResponseParameters().get("error_description")),
                    () -> assertFalse(authorizationResponse.toResponseParameters().containsKey("code"))
            );
        }

    }

    @DisplayName("When the iss parameter is not supported")
    @Nested
    class IssParameterNotSupportedTests {

        @Test
        @DisplayName("then the authorization response should not include the iss parameter")
        public void testIssParameterNotSupported() throws Exception {
            OpenIDConnectIntegration openIDConnectSdk = initSdk(false);
            MockRequest request = new MockRequest();
            request.addParameter("client_id", "c");
            PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
            Authorization authorization = Authorization.builder().sub("p").build();
            AuthorizationResponse authorizationResponse = openIDConnectSdk.authorize(pushedAuthorizationRequest, authorization);
            assertAll(
                    () -> assertNull(authorizationResponse.getIss()),
                    () -> assertNull(authorizationResponse.toResponseParameters().get("iss")),
                    () -> assertNotNull(authorizationResponse.getCode())
            );
        }

        @Test
        @DisplayName("then an error authorization response should not include the iss parameter")
        public void testErrorResponse() throws Exception {
            OpenIDConnectIntegration openIDConnectSdk = initSdk(false);
            MockRequest request = new MockRequest();
            PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
            AuthorizationResponse authorizationResponse = openIDConnectSdk.errorResponse(pushedAuthorizationRequest, "foo", "msg");
            assertAll(
                    () -> assertNull(authorizationResponse.getIss()),
                    () -> assertNull(authorizationResponse.toResponseParameters().get("iss")),
                    () -> assertNull(authorizationResponse.getCode()),
                    () -> assertEquals("foo", authorizationResponse.toResponseParameters().get("error")),
                    () -> assertEquals("msg", authorizationResponse.toResponseParameters().get("error_description")),
                    () -> assertFalse(authorizationResponse.toResponseParameters().containsKey("code"))
            );
        }


    }

}
