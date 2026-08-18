package no.idporten.eudiw.issuer.api.revoke;

import no.idporten.eudiw.issuer.context.CredentialRevokeContext;
import no.idporten.eudiw.issuer.credentials.status.StatusIssuerService;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("When revoking credentials")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class PreAuthCredentialRevokeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccessTokenValidationService accessTokenValidationService;

    @MockitoBean
    private StatusIssuerService statusIssuerService;

    @MockitoBean
    private AuditLogger auditLogger;

    @MockitoBean(name = "preAuthorizationRestClient")
    private RestClient restClient;

    @Captor
    private ArgumentCaptor<CredentialRevokeContext> credentialRevokeContextCaptor;

    private String revocationRequestBody(String credentialConfigurationId, String issuanceTransactionId) {
        return """
                {
                    "credential_configuration_id": "%s",
                    "issuance_transaction_id": "%s"
                }""".formatted(credentialConfigurationId, issuanceTransactionId);
    }

    @DisplayName("then a valid requests revokes credential")
    @Test
    void testRevokeIssuedCredential() throws Exception {
        IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId();
        mockMvc.perform(put("/api/v1/credential/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer access.token.sign")
                        .content(revocationRequestBody("junitdoc_pre_mso_mdoc", issuanceTransactionId.getValue()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        verify(statusIssuerService).revokeStatus(credentialRevokeContextCaptor.capture());
        CredentialRevokeContext credentialRevokeContext = credentialRevokeContextCaptor.getValue();
        assertAll(
                () -> assertEquals(issuanceTransactionId, credentialRevokeContext.transactionId()),
                () -> assertEquals("junitdoc_pre_mso_mdoc", credentialRevokeContext.credentialConfiguration().getCredentialConfigurationId())
        );
    }

}
