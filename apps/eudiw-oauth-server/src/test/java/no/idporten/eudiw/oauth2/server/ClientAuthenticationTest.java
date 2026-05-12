package no.idporten.eudiw.oauth2.server;

import no.idporten.eudiw.oauth2.server.client.ClientMetadata;
import no.idporten.eudiw.oauth2.server.config.OAuth2ServerConfiguration;
import no.idporten.eudiw.oauth2.server.protocol.AuthenticatedRequest;
import no.idporten.eudiw.oauth2.server.protocol.PushedAuthorizationRequest;
import no.idporten.eudiw.oauth2.server.protocol.TokenRequest;
import org.junit.jupiter.api.*;

import static no.idporten.eudiw.oauth2.server.TestUtils.basicAuthHeader;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When authenticating a client")
public class ClientAuthenticationTest {

    private OAuth2AuthorizationServerBase authorizationServer;

    @BeforeEach
    public void setUp() throws Exception {
        OAuth2ServerConfiguration serverConfiguration = TestUtils.defaultOAuth2ServerTestConfigurationBuilder()
                .client(ClientMetadata.builder().clientId("anotherclient").clientSecret("secret2").scope("openid").redirectUri("https://junit.idporten.no/").build())
                .build();
        authorizationServer = new OAuth2AuthorizationServerBase(serverConfiguration);
    }

    @DisplayName("with invalid use of client authentication")
    @Nested
    class InvalidUseOfClientAuthenticationTests {

        @Test
        @DisplayName("then missing client authentication raises an OAuth2 error with code invalid_client")
        public void testMissingClientAuthentication() {
            MockRequest request = new MockRequest();
            AuthenticatedRequest authenticatedRequest = new TokenRequest(request.getHeaders(), request.getParameters());
            try {
                authorizationServer.authenticateClient(authenticatedRequest);
                fail();
            } catch (OAuth2Exception e) {
                assertAll(
                        () -> assertEquals(OAuth2Exception.INVALID_CLIENT, e.error()),
                        () -> assertTrue(e.errorDescription().contains("Missing client authentication"))
                );
            }
        }

        @Test
        @DisplayName("then multiple client authentications raises an OAuth2 error with code invalid_client")
        public void testMultipleClientAuthentication() {
            MockRequest request = new MockRequest();
            request.addParameter("client_id", TestUtils.defaultClientMetadata().getClientId());
            request.addParameter("client_secret", TestUtils.defaultClientMetadata().getClientSecret());
            request.addHeader("Authorization", basicAuthHeader(TestUtils.defaultClientMetadata().getClientId(), TestUtils.defaultClientMetadata().getClientSecret()));
            AuthenticatedRequest authenticatedRequest = new TokenRequest(request.getHeaders(), request.getParameters());
            try {
                authorizationServer.authenticateClient(authenticatedRequest);
                fail();
            } catch (OAuth2Exception e) {
                assertAll(
                        () -> assertEquals(OAuth2Exception.INVALID_CLIENT, e.error()),
                        () -> assertTrue(e.errorDescription().contains("Multiple client authentications"))
                );
            }
        }

    }

    @DisplayName("using a client secret")
    @Nested
    class ClientSecretTests {

        @Test
        @DisplayName("then client authentication in header with a different client_id as parameter raises an OAuth2 error with code invalid_client and response code unauthorized")
        public void testMixedClientAuthentications() {
            MockRequest request = new MockRequest();
            request.addParameter("client_id", TestUtils.defaultClientMetadata().getClientId() + "x");
            request.addHeader("Authorization", basicAuthHeader(TestUtils.defaultClientMetadata().getClientId(), TestUtils.defaultClientMetadata().getClientSecret()));
            PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
            try {
                authorizationServer.process(pushedAuthorizationRequest);
                fail();
            } catch (OAuth2Exception e) {
                assertAll(
                        () -> assertEquals(OAuth2Exception.INVALID_CLIENT, e.error()),
                        () -> assertTrue(e.errorDescription().contains("Invalid client authentication")),
                        () -> assertTrue(e.errorDescription().contains("does not match parameter client_id")),
                        () -> assertEquals(401, e.getHttpStatusCode())
                );
            }
        }

        @Test
        @DisplayName("then authentication method client_secret_post can be used")
        public void testClientSecretPost() {
            MockRequest request = new MockRequest();
            ClientMetadata clientMetadata = TestUtils.defaultClientMetadata();
            request.addParameter("client_id", clientMetadata.getClientId());
            request.addParameter("client_secret", clientMetadata.getClientSecret());
            AuthenticatedRequest authenticatedRequest = new TokenRequest(request.getHeaders(), request.getParameters());
            ClientMetadata authenticatedClient = authorizationServer.authenticateClient(authenticatedRequest);
            assertAll(
                    () -> assertEquals(TestUtils.defaultClientMetadata().getClientId(), authenticatedClient.getClientId()),
                    () -> assertNull(authenticatedRequest.getClientSecret())
            );
        }

        @Test
        @DisplayName("then authentication method client_secret_basic can be used")
        public void testClientSecretBasic() {
            MockRequest request = new MockRequest();
            request.addParameter("client_id", TestUtils.defaultClientMetadata().getClientId());
            request.addHeader("Authorization", basicAuthHeader(TestUtils.defaultClientMetadata().getClientId(), TestUtils.defaultClientMetadata().getClientSecret()));
            AuthenticatedRequest authenticatedRequest = new TokenRequest(request.getHeaders(), request.getParameters());
            ClientMetadata clientMetadata = authorizationServer.authenticateClient(authenticatedRequest);
            assertAll(
                    () -> assertEquals(TestUtils.defaultClientMetadata().getClientId(), clientMetadata.getClientId()),
                    () -> assertNull(authenticatedRequest.getAuthorizationHeader())
            );
        }

        @Test
        @DisplayName("then an invalid authorization header raises an OAuth2 error with code invalid_client and response code unauthorized")
        public void testInvalidAuthorizationHeader() {
            MockRequest request = new MockRequest();
            request.addParameter("client_id", TestUtils.defaultClientMetadata().getClientId());
            request.addHeader("Authorization", "Basic æææ");
            AuthenticatedRequest authenticatedRequest = new TokenRequest(request.getHeaders(), request.getParameters());
            try {
                authorizationServer.authenticateClient(authenticatedRequest);
                fail();
            } catch (OAuth2Exception e) {
                assertAll(
                        () -> assertEquals(OAuth2Exception.INVALID_CLIENT, e.error()),
                        () -> assertTrue(e.errorDescription().contains("Invalid client authentication")),
                        () -> assertTrue(e.errorDescription().contains("Invalid Authorization header")),
                        () -> assertEquals(401, e.getHttpStatusCode())
                );
            }
        }

        @Test
        @DisplayName("then an invalid client_secret raises an OAuth2 error with code invalid_client and response code unauthorized")
        public void testInvalidClientSecret() {
            try {
                authorizationServer.authenticateClient(TestUtils.defaultClientMetadata().getClientId(), "x");
                fail();
            } catch (OAuth2Exception e) {
                assertAll(
                        () -> assertEquals(OAuth2Exception.INVALID_CLIENT, e.error()),
                        () -> assertTrue(e.errorDescription().contains("Invalid client authentication")),
                        () -> assertEquals(401, e.getHttpStatusCode())
                );
            }
        }

        @Test
        @DisplayName("then authentication method client_secret_jwt can be used")
        public void testClientSecretJwt() throws Exception {
            ClientMetadata clientMetadata = TestUtils.defaultClientMetadata();
            MockRequest request = new MockRequest();
            request.addParameter("client_assertion", TestUtils.createClientSecretJWT(clientMetadata, authorizationServer.getConfiguration().getIssuer().toString()).serialize());
            request.addParameter("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
            AuthenticatedRequest authenticatedRequest = new TokenRequest(request.getHeaders(), request.getParameters());
            ClientMetadata authenticatedClientMetadata = authorizationServer.authenticateClient(authenticatedRequest);
            assertAll(
                    () -> assertEquals(TestUtils.defaultClientMetadata().getClientId(), authenticatedClientMetadata.getClientId()),
                    () -> assertNull(authenticatedRequest.getClientSecret())
            );
        }

        @Test
        @DisplayName("then a jwt with invalid audience is rejected")
        public void testClientSecretJwtInvalidAudience() throws Exception {
            ClientMetadata clientMetadata = TestUtils.defaultClientMetadata();
            MockRequest request = new MockRequest();
            request.addParameter("client_assertion", TestUtils.createClientSecretJWT(clientMetadata, "foo-issuer").serialize());
            request.addParameter("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
            AuthenticatedRequest authenticatedRequest = new TokenRequest(request.getHeaders(), request.getParameters());
            OAuth2Exception exception = assertThrows(OAuth2Exception.class, () -> authorizationServer.authenticateClient(authenticatedRequest));
            assertAll(
                    () -> assertEquals("invalid_client", exception.error()),
                    () -> assertTrue(exception.errorDescription().contains("Unknown JWT audience")),
                    () -> assertEquals(401, exception.getHttpStatusCode())
            );
        }

        @Test
        @DisplayName("then a jwt with no audience is rejected")
        public void testClientSecretJwtNoAudience() throws Exception {
            ClientMetadata clientMetadata = TestUtils.defaultClientMetadata();
            MockRequest request = new MockRequest();
            request.addParameter("client_assertion", TestUtils.createClientSecretJWT(clientMetadata).serialize());
            request.addParameter("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
            AuthenticatedRequest authenticatedRequest = new TokenRequest(request.getHeaders(), request.getParameters());
            OAuth2Exception exception = assertThrows(OAuth2Exception.class, () -> authorizationServer.authenticateClient(authenticatedRequest));
            assertAll(
                    () -> assertEquals("invalid_client", exception.error()),
                    () -> assertTrue(exception.errorDescription().contains("Missing JWT audience")),
                    () -> assertEquals(401, exception.getHttpStatusCode())
            );
        }

        @Test
        @DisplayName("then a jwt with multiple audiences is rejected")
        public void testClientSecretJwtMultipleAudience() throws Exception {
            ClientMetadata clientMetadata = TestUtils.defaultClientMetadata();
            MockRequest request = new MockRequest();
            request.addParameter("client_assertion", TestUtils.createClientSecretJWT(clientMetadata, "aud1", "aud2").serialize());
            request.addParameter("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
            AuthenticatedRequest authenticatedRequest = new TokenRequest(request.getHeaders(), request.getParameters());
            OAuth2Exception exception = assertThrows(OAuth2Exception.class, () -> authorizationServer.authenticateClient(authenticatedRequest));
            assertAll(
                    () -> assertEquals("invalid_client", exception.error()),
                    () -> assertTrue(exception.errorDescription().contains("Unique JWT audience required")),
                    () -> assertEquals(401, exception.getHttpStatusCode())
            );
        }

        @Test
        @DisplayName("then an unknown client_id raises an OAuth2 error with code invalid_client and response code unauthorized")
        public void testUnknownClient() {
            try {
                authorizationServer.authenticateClient("unknown", "secret");
                fail();
            } catch (OAuth2Exception e) {
                assertAll(
                        () -> assertEquals(OAuth2Exception.INVALID_CLIENT, e.error()),
                        () -> assertTrue(e.errorDescription().contains("Invalid client authentication")),
                        () -> assertTrue(e.errorDescription().contains("Unknown client")),
                        () -> assertEquals(401, e.getHttpStatusCode())
                );
            }
        }

    }

    @DisplayName("using an attestation (wia)")
    @Nested
    class AttestationBasedTests {

        @Test
        @Disabled // TODO litt usikker på hvordan best mocke data for tester, lager egen sak på det.
        public void testSuccessfulAuthentication() throws Exception {
            String clientId = "eudiw-abca";
            String clientAttestation = "eyJ0eXAiOiJvYXV0aC1jbGllbnQtYXR0ZXN0YXRpb24rand0IiwiYWxnIjoiRVMyNTYiLCJ4NWMiOlsiTUlJRERUQ0NBclNnQXdJQkFnSVVVQktUTzJHeWdSMmUrWWxGR21xL2NjU3dacHd3Q2dZSUtvWkl6ajBFQXdJd1hERWVNQndHQTFVRUF3d1ZVRWxFSUVsemMzVmxjaUJEUVNBdElGVlVJREF5TVMwd0t3WURWUVFLRENSRlZVUkpJRmRoYkd4bGRDQlNaV1psY21WdVkyVWdTVzF3YkdWdFpXNTBZWFJwYjI0eEN6QUpCZ05WQkFZVEFsVlVNQjRYRFRJMU1UQXdPVEE0TkRRd05sb1hEVEkzTVRBd09UQTRORFF3TlZvd1ZqRW1NQ1FHQTFVRUF3d2RaR1YyTG5kaGJHeGxkQzF3Y205MmFXUmxjaTVsZFdScGR5NWtaWFl4RHpBTkJnTlZCQVVUQmxkUVUwUkZWakVPTUF3R0ExVUVDZ3dGVG1sVFkza3hDekFKQmdOVkJBWVRBbFZVTUZrd0V3WUhLb1pJemowQ0FRWUlLb1pJemowREFRY0RRZ0FFOTUya1B3NWdSdXRtQXcvUlNGcWhFdld6enZwN25PLzJPYzdXN2xPK1duYi9WdXZUaDhEU2h5MU0wczNKV3g5Umh5OFQ2UTdjcVU1SHpYcDlPZHJzQjZPQ0FWZ3dnZ0ZVTUF3R0ExVWRFd0VCL3dRQ01BQXdId1lEVlIwakJCZ3dGb0FVWXNlVVJ5aTlENklXSUtlYXdrbVVSUEVCMDhjd1BBWURWUjBSQkRVd000RVNibTh0Y21Wd2JIbEFaWFZrYVhjdVpHVjJnaDFrWlhZdWQyRnNiR1YwTFhCeWIzWnBaR1Z5TG1WMVpHbDNMbVJsZGpBU0JnTlZIU1VFQ3pBSkJnY29nWXhkQlFFR01FTUdBMVVkSHdROE1Eb3dPS0Eyb0RTR01taDBkSEJ6T2k4dmNISmxjSEp2WkM1d2Eya3VaWFZrYVhjdVpHVjJMMk55YkM5d2FXUmZRMEZmVlZSZk1ESXVZM0pzTUIwR0ExVWREZ1FXQkJTOTdLNHgvMk5ObDVxQ2ZncEFOMU5SWmNIeUNqQU9CZ05WSFE4QkFmOEVCQU1DQjRBd1hRWURWUjBTQkZZd1ZJWlNhSFIwY0hNNkx5OW5hWFJvZFdJdVkyOXRMMlYxTFdScFoybDBZV3d0YVdSbGJuUnBkSGt0ZDJGc2JHVjBMMkZ5WTJocGRHVmpkSFZ5WlMxaGJtUXRjbVZtWlhKbGJtTmxMV1p5WVcxbGQyOXlhekFLQmdncWhrak9QUVFEQWdOSEFEQkVBaUFUbEhUZnBZRWJHTVFkMkRYbTlHSGVKcXFEUG9XUGNLSDJjZnRXWE5EYVlRSWdUK1l5QTgya0EyaytMUmJqZTBvRmp4VTAwSjh2d3VFcjFmN3dTOGxGdkZrPSJdfQ.ewogICAgImlzcyI6ICJodHRwczovL2Rldi53YWxsZXQtcHJvdmlkZXIuZXVkaXcuZGV2IiwKICAgICJzdWIiOiAiZXVkaXctYWJjYSIsCiAgICAiZXhwIjogMTc3ODI0MDMxMSwKICAgICJjbmYiOiB7CiAgICAgICAgImp3ayI6IHsKICAgICAgICAgICAgImFsZyI6ICJFUzI1NiIsCiAgICAgICAgICAgICJjcnYiOiAiUC0yNTYiLAogICAgICAgICAgICAia2lkIjogImNsaWVudC1hdHRlc3RhdGlvbi03MjI2YjUxZmQ4MzU2OWZkIiwKICAgICAgICAgICAgImt0eSI6ICJFQyIsCiAgICAgICAgICAgICJ1c2UiOiAic2lnIiwKICAgICAgICAgICAgIngiOiAidnpPaEY5Sk53U3h1ZjA5elZHUDdMaklfZzlRbDY0SFRkalBoNHFIMGVxTSIsCiAgICAgICAgICAgICJ5IjogIm1ZQnFCUnRHUkZHMzVkMXBtQVZ6Wm9haGNFbWJEWEppdzc3bW5XUjZZajQiCiAgICAgICAgfQogICAgfSwKICAgICJpYXQiOiAxNzc4MjQwMDExLAogICAgIm5iZiI6IDE3NzgyNDAwMTEsCiAgICAid2FsbGV0X25hbWUiOiAiRVVESSBXYWxsZXQiLAogICAgIndhbGxldF9saW5rIjogImh0dHBzOi8vZWMuZXVyb3BhLmV1L2RpZ2l0YWwtYnVpbGRpbmctYmxvY2tzL3NpdGVzL3NwYWNlcy9FVURJR0lUQUxJREVOVElUWVdBTExFVC9wYWdlcy82OTQ0ODc3MzgvRVUrRGlnaXRhbCtJZGVudGl0eStXYWxsZXQrSG9tZSIsCiAgICAiZXVkaV93YWxsZXRfaW5mbyI6IHsKICAgICAgICAiZ2VuZXJhbF9pbmZvIjogewogICAgICAgICAgICAid2FsbGV0X3Byb3ZpZGVyX25hbWUiOiAiREcgQ29ubmVjdCIsCiAgICAgICAgICAgICJ3YWxsZXRfc29sdXRpb25faWQiOiAiRVVESSBXYWxsZXQiLAogICAgICAgICAgICAid2FsbGV0X3NvbHV0aW9uX3ZlcnNpb24iOiAiMS4wIiwKICAgICAgICAgICAgIndhbGxldF9zb2x1dGlvbl9jZXJ0aWZpY2F0aW9uX2luZm9ybWF0aW9uIjogIkFSRiIKICAgICAgICB9CiAgICB9Cn0.9Rioq59Hxb-LmEM_QZZ0OkZe2MkKPTeOUlC8b2YayRyD_BeBMcsihj-vGGvT1OOKEyOcYhLLm9F2lQF4JW0Hvw";
            String clientAttestationPoP = "eyJhbGciOiJFUzI1NiIsInR5cCI6Im9hdXRoLWNsaWVudC1hdHRlc3RhdGlvbi1wb3Arand0In0.eyJjbmYiOnsiandrIjp7Imt0eSI6IkVDIiwiYWxnIjoiRVMyNTYiLCJ5IjoibVlCcUJSdEdSRkczNWQxcG1BVnpab2FoY0VtYkRYSml3NzdtbldSNllqNCIsImtpZCI6ImNsaWVudC1hdHRlc3RhdGlvbi03MjI2YjUxZmQ4MzU2OWZkIiwiY3J2IjoiUC0yNTYiLCJ1c2UiOiJzaWciLCJ4IjoidnpPaEY5Sk53U3h1ZjA5elZHUDdMaklfZzlRbDY0SFRkalBoNHFIMGVxTSJ9fSwiYXVkIjoiaHR0cDpcL1wvb2F1dGgtc2VydmVyOjkyNjAiLCJpYXQiOjE3NzgyNDAwMTQuNzMwNjcyOCwiaXNzIjoiZXVkaXctYWJjYSIsImp0aSI6ImJKTDN6azhUeGNXMHRlRDljN1pEIiwiY2hhbGxlbmdlIjoiNFNPLVF2dnI4djd3UGpFOWd2a1VZb1BBamRjUHI3M083V3dydFhTM0pzVSIsImV4cCI6MTc3ODI0MDMxNC43MzA2NzR9.NKBmlVwQR6A15Ts0MQA4kwLRh4hqZUJmCLEmNrwRY4XMB2XiMFMUAgOBAlOk6V81KYvAL7GNgD3vwQYLskJQ6g";
            ClientMetadata clientMetadata = authorizationServer.authenticateClientByAttestation(clientId, clientAttestation, clientAttestationPoP);
            assertEquals(clientId, clientMetadata.getClientId());
        }

    }

}
