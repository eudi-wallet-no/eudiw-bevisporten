package no.idporten.eudiw.issuer.api.revoke;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.context.CredentialRevokeContext;
import no.idporten.eudiw.issuer.credentials.status.StatusIssuerService;
import no.idporten.eudiw.issuer.issuance.authz.SubjectCredentialIssuanceTransactionEntity;
import no.idporten.eudiw.issuer.issuance.authz.SubjectCredentialTransactionDao;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.oauth2.InvalidAccessTokenException;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("When revoking authorization code credentials by subject")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
@ExtendWith(MockitoExtension.class)
class AuthCodeCredentialRevokeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubjectCredentialTransactionDao subjectCredentialTransactionDao;

    @MockitoBean
    private StatusIssuerService statusIssuerService;

    @MockitoBean
    private AccessTokenValidationService accessTokenValidationService;

    @MockitoBean
    private AuditLogger auditLogger;

    @MockitoBean(name = "preAuthorizationRestClient")
    private RestClient restClient;

    @Captor
    private ArgumentCaptor<CredentialRevokeContext> credentialRevokeContextCaptor;

    private String revokeRequestBody(String credentialConfigurationId, String subjectIdentifier) {
        return """
                {
                    "credential_configuration_id": "%s",
                    "subject_identifier": "%s"
                }""".formatted(credentialConfigurationId, subjectIdentifier);
    }

    @Nested
    @DisplayName("Test auth-code credential revoke")
    class HappyPath {

        @Test
        @DisplayName("revokes all matching transactions")
        void revokeAllMatchingTransactions() throws Exception {
            when(accessTokenValidationService.validateAccessToken(any())).thenReturn(org.mockito.Mockito.mock(JWT.class));
            when(subjectCredentialTransactionDao.findBySubjectAndType(eq("12345678901"), eq("junitdoc_pre_mso_mdoc"), anyString()))
                    .thenReturn(List.of(
                            subjectCredentialIssuanceTransactionEntity("transaction-1", "12345678901", "junitdoc_pre_mso_mdoc", "junit"),
                            subjectCredentialIssuanceTransactionEntity("transaction-2", "12345678901", "junitdoc_pre_mso_mdoc", "junit")
                    ));

            mockMvc.perform(put("/api/v1/credential/revoke/by-subject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "******")
                            .content(revokeRequestBody("junitdoc_pre_mso_mdoc", "12345678901"))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verify(statusIssuerService, times(2)).revokeStatus(credentialRevokeContextCaptor.capture());
            List<CredentialRevokeContext> capturedContexts = credentialRevokeContextCaptor.getAllValues();
            assertEquals("transaction-1", capturedContexts.get(0).transactionId().getValue());
            assertEquals("transaction-2", capturedContexts.get(1).transactionId().getValue());
        }

        @Test
        @DisplayName("does nothing when there are no matches")
        void noMatchingTransactions() throws Exception {
            when(accessTokenValidationService.validateAccessToken(any())).thenReturn(org.mockito.Mockito.mock(JWT.class));
            when(subjectCredentialTransactionDao.findBySubjectAndType(eq("12345678901"), eq("junitdoc_pre_mso_mdoc"), anyString()))
                    .thenReturn(List.of());

            mockMvc.perform(put("/api/v1/credential/revoke/by-subject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "******")
                            .content(revokeRequestBody("junitdoc_pre_mso_mdoc", "12345678901"))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            verifyNoInteractions(statusIssuerService);
        }
    }

    @Nested
    @DisplayName("Validation and authorization")
    class ValidationAndAuthorization {

        @Test
        @DisplayName("returns bad request when subject identifier is missing")
        void missingSubjectIdentifier() throws Exception {
            mockMvc.perform(put("/api/v1/credential/revoke/by-subject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "******")
                            .content("""
                                    {
                                        "credential_configuration_id": "junitdoc_pre_mso_mdoc"
                                    }""")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(subjectCredentialTransactionDao);
            verifyNoInteractions(statusIssuerService);
        }

        @Test
        @DisplayName("returns unauthorized when subject binding is invalid")
        void invalidSubjectBinding() throws Exception {
            when(accessTokenValidationService.validateAccessToken(any())).thenReturn(org.mockito.Mockito.mock(JWT.class));
            doThrow(new InvalidAccessTokenException("Token and request subject/person identifier does not match."))
                    .when(accessTokenValidationService)
                    .validateAccessTokenBoundToSubject(any(), eq("12345678901"));

            mockMvc.perform(put("/api/v1/credential/revoke/by-subject")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "******")
                            .content(revokeRequestBody("junitdoc_pre_mso_mdoc", "12345678901"))
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(subjectCredentialTransactionDao);
            verifyNoInteractions(statusIssuerService);
        }
    }

    private SubjectCredentialIssuanceTransactionEntity subjectCredentialIssuanceTransactionEntity(
            String issuanceTransactionId,
            String subjectIdentifier,
            String credentialConfigurationId,
            String credentialIssuerTenant
    ) {
        return new SubjectCredentialIssuanceTransactionEntity(
                1L,
                subjectIdentifier,
                issuanceTransactionId,
                System.currentTimeMillis(),
                1L,
                credentialConfigurationId,
                credentialIssuerTenant,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                "credential_issued",
                "notification-id"
        );
    }
}
