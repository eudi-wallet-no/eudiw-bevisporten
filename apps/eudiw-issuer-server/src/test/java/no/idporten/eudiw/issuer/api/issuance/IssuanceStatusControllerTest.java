package no.idporten.eudiw.issuer.api.issuance;

import no.idporten.eudiw.issuer.oauth2.AccessTokenValidator;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServer;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServerService;
import no.idporten.eudiw.issuer.issuance.status.CredentialIssuanceStatusService;
import no.idporten.eudiw.issuer.issuance.status.IssuanceStatus;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.logging.audit.AuditLogger;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("When polling for credential issuance status")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
public class IssuanceStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CredentialIssuanceStatusService credentialIssuanceStatusService;

    @MockitoBean
    private AuthorizationServerService authorizationServerService;

    @MockitoBean
    private AuditLogger auditLogger;

    @MockitoBean(name = "preAuthorizationRestClient")
    private RestClient restClient;

    private String sampleBearerToken() {
        return "eyJraWQiOiIyUEhBMTVBM1UzMkFBMFJaOEp2ZGhqVUwxYjVOckV1eHlCNVVibGtMdk9nIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiIxNTgxNTg5ODk0NyIsImNsbSI6WyJhZ2Vfb3Zlcl8xOCIsImJpcnRoX2RhdGUiLCJiaXJ0aF9wbGFjZSIsInBpZF9leHBpcnlfZGF0ZSIsImdpdmVuX25hbWUiLCJuYXRpb25hbGl0eSIsImlzc3VpbmdfY291bnRyeSIsImlzc3VpbmdfYXV0aG9yaXR5IiwiZmFtaWx5X25hbWUiXSwiaXNzIjoiaHR0cHM6Ly9pZHBvcnRlbi5kZXYiLCJjbGllbnRfYW1yIjoiY2xpZW50X3NlY3JldF9iYXNpYyIsInBpZCI6IjE1ODE1ODk4OTQ3IiwiY2xpZW50X2lkIjoiZGVtb2NsaWVudF9pZHBvcnRlbl9zeXN0ZXN0IiwiYWNyIjoiaWRwb3J0ZW4tbG9hLXN1YnN0YW50aWFsIiwic2NvcGUiOiJvcGVuaWQgcHJvZmlsZSBub2JpZDpwaWQiLCJleHAiOjE3NDc3NDU0MzEsImlhdCI6MTc0Nzc0NTMxMSwianRpIjoiYmNxNHJyVVM1c1kiLCJjb25zdW1lciI6eyJhdXRob3JpdHkiOiJpc282NTIzLWFjdG9yaWQtdXBpcyIsIklEIjoiMDE5Mjo5OTE4MjU4MjcifX0.rBA7QuJiCj__oMHIMp1zVDJ_lDqkaewEJk0iTOrEEehL7j4waCgprCNTB4NBMmi0Wya2Bl5DvAbSwsSQDI5wSRWnTEWoy9-CPwIPBDUvkhSoVG_8gvNwzFI8_Dh8iKzXzQiAOdYRRiQsLS-hcoqs0vb6VCf-kf_PHvhxwAKfA-ocPUangCMLUzOeJLjNA4o-ybIj0ZkOrwR0n8lJ3-AA2Bt6t4UDVcvdLgkP5VLj_AHL6ceoc4jE8e8yGM76hMFcfz0rOKOWGA5yZH2s08Z3ocW5Q9LDdXqQfizQ5lGFgjC4qJ72e1F_W-aBnuKchkFzgfTDk_n450IbGhjpjprrZHP2_33mwlauD2kZw8-OBGTOk41d5VdRxUkii-g67QcYYvWgqRwbZGQCejIEtSrkjPkyhMqQSWFk4WZU0LI0KE0JvA4e3AYo1A8rtDilRed-HeHNTHhD25QCO_pYvkszR2CPUq9LxlgD2FSzmp6GuQT-dpS9R0VQvtgSOQzjwQsNsDt18sN-9LEJxcRRGf5BV2asMfZuuxzckvkLR_cOX1FoscPdKvuxxEgZqD0q1neLljGr6-5LtX_ipHSExqQ2-HyPqUtF6d3toPlLTdpVqw3i_SehBdXW2gnoXsraI1KTY7IZwQOjeclqp9m937m96LSGQ2RRX4jnkAHunJwXUl0";
    }

    private void setUpAccessTokenValidation() {
        AccessTokenValidator accessTokenValidator = mock(AccessTokenValidator.class);
        when(accessTokenValidator.validate(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
        AuthorizationServer authorizationServer = mock(AuthorizationServer.class);
        when(authorizationServer.getIssuer()).thenReturn(URI.create("https://idporten.dev"));
        when(authorizationServer.getAccessTokenValidator()).thenReturn(accessTokenValidator);
        when(authorizationServerService.findAuthorizationServer(any(), any())).thenReturn(authorizationServer);
        when(authorizationServerService.getPreAuthorizationServers()).thenReturn(List.of(authorizationServer));
    }


    @DisplayName("then requests without access_token is rejected")
    @Test
    void testMissingAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/v1/credential/issuance-transaction/foo")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_description").value(Matchers.containsString("Missing authorization header")));
        verifyNoInteractions(credentialIssuanceStatusService);
    }

    @DisplayName("then a valid requests returns the credential issuance status")
    @Test
    void testReturnCredentialIssuanceStatus() throws Exception {
        setUpAccessTokenValidation();
        IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId();
        when(credentialIssuanceStatusService.getIssuanceStatus(any(), eq(issuanceTransactionId))).thenReturn(new IssuanceStatus(issuanceTransactionId, "foo", "credential_tested"));
        mockMvc.perform(get("/api/v1/credential/issuance-transaction/{}}", issuanceTransactionId)
                        .header("Authorization", "Bearer %s".formatted(sampleBearerToken()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("credential_tested"))
                .andExpect(jsonPath("$.issuance_transaction_id").value(issuanceTransactionId.getValue()));
    }

}
