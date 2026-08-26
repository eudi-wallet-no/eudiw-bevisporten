package no.idporten.eudiw.issuer.api.revoke;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.credentials.status.StatusIssuerService;
import no.idporten.eudiw.issuer.issuance.authz.SubjectCredentialTransactionDao;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("When the revocation result endpoint is disabled")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest(properties = "credential-issuer-server.features.revocation-result-endpoint.enabled=false")
class RevocationResultEndpointFeatureDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccessTokenValidationService accessTokenValidationService;

    @MockitoBean
    private SubjectCredentialTransactionDao subjectCredentialTransactionDao;

    @MockitoBean
    private StatusIssuerService statusIssuerService;

    @MockitoBean
    private AuditLogger auditLogger;

    @MockitoBean(name = "preAuthorizationRestClient")
    private RestClient restClient;

    @Test
    @DisplayName("then the pre-authorized v2 endpoint returns not found")
    void preAuthorizedV2EndpointIsUnavailable() throws Exception {
        mockMvc.perform(put("/api/v2/credential/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "******")
                        .content(preAuthorizedRequestBody())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verifyNoInteractions(statusIssuerService);
    }

    @Test
    @DisplayName("then the authorization code v2 endpoint returns not found")
    void authorizationCodeV2EndpointIsUnavailable() throws Exception {
        mockMvc.perform(put("/api/v2/credential/revoke/by-subject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "******")
                        .content(bySubjectRequestBody())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verifyNoInteractions(statusIssuerService);
    }

    @Test
    @DisplayName("then both v1 endpoints remain available")
    void v1EndpointsRemainAvailable() throws Exception {
        when(accessTokenValidationService.validateAccessToken(any())).thenReturn(mock(JWT.class));
        when(subjectCredentialTransactionDao.findBySubjectAndType(any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(put("/api/v1/credential/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "******")
                        .content(preAuthorizedRequestBody()))
                .andExpect(status().isNoContent());

        mockMvc.perform(put("/api/v1/credential/revoke/by-subject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "******")
                        .content(bySubjectRequestBody()))
                .andExpect(status().isNoContent());
    }

    private String preAuthorizedRequestBody() {
        return """
                {
                    "credential_configuration_id": "junitdoc_pre_mso_mdoc",
                    "issuance_transaction_id": "26866cb5-4129-43c9-9231-66c2d75556bf"
                }""";
    }

    private String bySubjectRequestBody() {
        return """
                {
                    "credential_configuration_id": "junitdoc_pre_mso_mdoc",
                    "subject": {
                        "identifier": "12345678901"
                    }
                }""";
    }
}
