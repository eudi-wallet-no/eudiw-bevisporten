package no.idporten.eudiw.oauth2.server;

import no.idporten.eudiw.oauth2.server.client.ClientMetadata;
import no.idporten.eudiw.oauth2.server.config.OAuth2ServerConfiguration;
import no.idporten.eudiw.oauth2.server.protocol.Authorization;
import no.idporten.eudiw.oauth2.server.protocol.AuthorizationResponse;
import no.idporten.eudiw.oauth2.server.protocol.PushedAuthorizationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthorizationResponseGenerationTest {

    OAuth2AuthorizationServer initServer(boolean authorizationResponseIssParameterSupported) throws Exception {
        ClientMetadata client1 = ClientMetadata.builder().clientId("client1").clientSecret("secret").scope("openid").redirectUri("https://junit.idporten.no/").build();
        OAuth2ServerConfiguration configuration = TestUtils.defaultOAuth2ServerTestConfigurationBuilder()
                .client(client1)
                .authorizationDetailsTypeSupported("foo")
                .authorizationResponseIssParameterSupported(authorizationResponseIssParameterSupported)
                .build();
        return new OAuth2AuthorizationServerBase(configuration);
    }

    @DisplayName("When the iss parameter is supported")
    @Nested
    class IssParameterSupportedTests {

        @Test
        @DisplayName("then an authorization response should include the iss parameter")
        public void testAuthorize() throws Exception {
            OAuth2AuthorizationServer oAuth2AuthorizationServer = initServer(true);
            MockRequest request = new MockRequest();
            request.addParameter("client_id", "c");
            PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
            Authorization authorization = Authorization.builder().sub("p").build();
            AuthorizationResponse authorizationResponse = oAuth2AuthorizationServer.authorize(pushedAuthorizationRequest, authorization);
            assertAll(
                    () -> assertEquals(oAuth2AuthorizationServer.getConfiguration().getIssuer().toString(), authorizationResponse.toResponseParameters().get("iss")),
                    () -> assertEquals(oAuth2AuthorizationServer.getConfiguration().getIssuer().toString(), authorizationResponse.getIss()),
                    () -> assertNotNull(authorizationResponse.getCode()),
                    () -> assertEquals(authorizationResponse.getCode(), authorizationResponse.toResponseParameters().get("code")),
                    () -> assertFalse(authorizationResponse.toResponseParameters().containsKey("error"))
            );
        }

        @Test
        @DisplayName("then an error authorization response should include the iss parameter")
        public void testErrorResponse() throws Exception {
            OAuth2AuthorizationServer authorizationServer = initServer(true);
            MockRequest request = new MockRequest();
            PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
            AuthorizationResponse authorizationResponse = authorizationServer.errorResponse(pushedAuthorizationRequest, "foo", "msg");
            assertAll(
                    () -> assertEquals(authorizationServer.getConfiguration().getIssuer().toString(), authorizationResponse.toResponseParameters().get("iss")),
                    () -> assertEquals(authorizationServer.getConfiguration().getIssuer().toString(), authorizationResponse.getIss()),
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
            OAuth2AuthorizationServer authorizationServer = initServer(false);
            MockRequest request = new MockRequest();
            request.addParameter("client_id", "c");
            PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
            Authorization authorization = Authorization.builder().sub("p").build();
            AuthorizationResponse authorizationResponse = authorizationServer.authorize(pushedAuthorizationRequest, authorization);
            assertAll(
                    () -> assertNull(authorizationResponse.getIss()),
                    () -> assertNull(authorizationResponse.toResponseParameters().get("iss")),
                    () -> assertNotNull(authorizationResponse.getCode())
            );
        }

        @Test
        @DisplayName("then an error authorization response should not include the iss parameter")
        public void testErrorResponse() throws Exception {
            OAuth2AuthorizationServer authorizationServer = initServer(false);
            MockRequest request = new MockRequest();
            PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
            AuthorizationResponse authorizationResponse = authorizationServer.errorResponse(pushedAuthorizationRequest, "foo", "msg");
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
