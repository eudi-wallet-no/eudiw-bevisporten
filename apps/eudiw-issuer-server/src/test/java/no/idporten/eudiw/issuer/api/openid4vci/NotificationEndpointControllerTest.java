package no.idporten.eudiw.issuer.api.openid4vci;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.ErrorCode;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.issuance.status.CredentialIssuanceStatusService;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidator;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServer;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServerService;
import no.idporten.eudiw.issuer.oauth2.InvalidAccessTokenException;
import no.idporten.eudiw.issuer.openid4vci.notification.NotificationId;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("When using the notification endpoint, then access token validation is expected")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
public class NotificationEndpointControllerTest {

    private static final String ENDPOINT = "/openid4vci/notification";
    private static final String ISSUER = "https://junit.idporten.no";
    private static final String SAMPLE_ACCESS_TOKEN = "eyJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJodHRwczovL2p1bml0LmlkcG9ydGVuLm5vIn0.dGVzdC1zaWduYXR1cmU";
    private static final String VALID_REQUEST = """
            {
              "notification_id": "abc123",
              "event": "credential_accepted"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CredentialIssuanceStatusService credentialIssuanceStatusService;

    @MockitoBean
    private AuditLogger auditLogger;

    @MockitoBean
    private AuthorizationServerService authorizationServerService;

    @MockitoBean(name = "preAuthorizationRestClient")
    private RestClient restClient;

    private AccessTokenValidator accessTokenValidator;
    private AuthorizationServer authorizationServer;

    @BeforeEach
    void setUpAccessTokenValidation() {
        accessTokenValidator = mock(AccessTokenValidator.class);
        when(accessTokenValidator.validate(any(), any())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
        authorizationServer = mock(AuthorizationServer.class);
        when(authorizationServer.getAccessTokenValidator()).thenReturn(accessTokenValidator);
        when(authorizationServerService.findAuthorizationServerByIssuer(any(), any())).thenReturn(authorizationServer);
        when(authorizationServerService.getPrimaryAuthorizationServer()).thenReturn(authorizationServer);
    }

    @DisplayName("When token validation succeeds, then a status update is expected")
    @ParameterizedTest
    @CsvSource({
            "/openid4vci/notification, https://junit.eidas2sandkasse.dev",
            "/junit/openid4vci/notification, https://junit.eidas2sandkasse.dev/junit"
    })
    void testWalletUpdatesStatus(String endpoint, String audience) throws Exception {
        mockMvc.perform(notificationRequest(endpoint)
                        .header("Authorization", "Bearer %s".formatted(SAMPLE_ACCESS_TOKEN)))
                .andExpect(status().isNoContent());
        verify(authorizationServerService).findAuthorizationServerByIssuer(eq(ISSUER), eq(List.of(authorizationServer)));
        ArgumentCaptor<JWT> validatedToken = ArgumentCaptor.forClass(JWT.class);
        ArgumentCaptor<JWT> forwardedToken = ArgumentCaptor.forClass(JWT.class);
        ArgumentCaptor<CredentialIssuerTenant> tenant = ArgumentCaptor.forClass(CredentialIssuerTenant.class);
        verify(accessTokenValidator).validate(validatedToken.capture(), eq(URI.create(audience)));
        verify(credentialIssuanceStatusService).walletStatusUpdated(tenant.capture(), eq(new NotificationId("abc123")), eq("credential_accepted"), forwardedToken.capture());
        assertSame(validatedToken.getValue(), forwardedToken.getValue());
        assertEquals(URI.create(audience), tenant.getValue().getCredentialIssuer());
    }

    @DisplayName("When credential configuration validation fails, then its authorization error is expected")
    @ParameterizedTest
    @EnumSource(value = ErrorCode.class, names = {"INVALID_TOKEN", "INSUFFICIENT_SCOPE"})
    void testCredentialConfigurationValidationFailure(ErrorCode errorCode) throws Exception {
        doThrow(new IssuerServerException(errorCode, "Token does not authorize this credential configuration."))
                .when(credentialIssuanceStatusService).walletStatusUpdated(any(), any(), any(), any());
        mockMvc.perform(notificationRequest(ENDPOINT)
                        .header("Authorization", "Bearer %s".formatted(SAMPLE_ACCESS_TOKEN)))
                .andExpect(status().is(errorCode.httpStatus().value()))
                .andExpect(jsonPath("$.error").value(errorCode.error()));
    }

    @DisplayName("When authorization is missing, then rejection without a status update is expected")
    @ParameterizedTest
    @ValueSource(strings = {ENDPOINT, "/junit/openid4vci/notification"})
    void testMissingAuthorization(String endpoint) throws Exception {
        mockMvc.perform(notificationRequest(endpoint))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_request"));
        verifyNoInteractions(credentialIssuanceStatusService);
    }

    @DisplayName("When authorization is malformed, then an invalid token response is expected")
    @ParameterizedTest
    @ValueSource(strings = {"DPoP not-a-jwt", "Basic invalid"})
    void testMalformedAuthorization(String authorization) throws Exception {
        mockMvc.perform(notificationRequest(ENDPOINT).header("Authorization", authorization))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_token"));
        verifyNoInteractions(credentialIssuanceStatusService);
    }

    @DisplayName("When token validation fails, then rejection without a status update is expected")
    @Test
    void testInvalidAccessToken() throws Exception {
        when(accessTokenValidator.validate(any(), any())).thenThrow(new InvalidAccessTokenException("Invalid access token."));
        mockMvc.perform(notificationRequest(ENDPOINT)
                        .header("Authorization", "Bearer %s".formatted(SAMPLE_ACCESS_TOKEN)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_token"));
        verifyNoInteractions(credentialIssuanceStatusService);
    }

    @DisplayName("When a DPoP token has no proof, then rejection without a status update is expected")
    @Test
    void testMissingDpopProof() throws Exception {
        mockMvc.perform(notificationRequest(ENDPOINT)
                        .header("Authorization", "DPoP %s".formatted(SAMPLE_ACCESS_TOKEN)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_request"));
        verifyNoInteractions(credentialIssuanceStatusService);
    }

    @DisplayName("When a DPoP proof is malformed, then rejection without a status update is expected")
    @Test
    void testInvalidDpopProof() throws Exception {
        mockMvc.perform(notificationRequest(ENDPOINT)
                        .header("Authorization", "DPoP %s".formatted(SAMPLE_ACCESS_TOKEN))
                        .header("DPoP", "invalid-proof"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_dpop_proof"));
        verifyNoInteractions(credentialIssuanceStatusService);
    }

    @DisplayName("When an authenticated request has an invalid event, then a notification error is expected")
    @Test
    void testInvalidRequest() throws Exception {
        mockMvc.perform(notificationRequest(ENDPOINT)
                        .header("Authorization", "Bearer %s".formatted(SAMPLE_ACCESS_TOKEN))
                        .content("""
                                {
                                "notification_id": "abc123",
                                "event": "credential_ignored"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_notification_request"));
        verifyNoInteractions(credentialIssuanceStatusService);
    }

    private MockHttpServletRequestBuilder notificationRequest(String endpoint) {
        return post(endpoint).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST);
    }

}
