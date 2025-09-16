package no.idporten.eudiw.issuer.api;


import no.idporten.eudiw.issuer.openid4vci.service.NotificationId;
import no.idporten.eudiw.issuer.openid4vci.service.CredentialIssuanceStatusService;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("When wallet is using the notification endpoint")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
public class NotificationEndpointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CredentialIssuanceStatusService credentialIssuanceStatusService;

    @MockitoBean
    private AuditLogger auditLogger;

    @DisplayName("then notification status for the credential issuance is updated")
    @Test
    void testWalletUpdatesStatus() throws Exception {
        mockMvc.perform(post("/openid4vci/notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                "notification_id": "abc123",
                                "event": "credential_accepted"
                                }
                                """))
                .andExpect(status().isNoContent());
        verify(credentialIssuanceStatusService).walletStatusUpdated(eq(new NotificationId("abc123")), eq("credential_accepted"));
    }

    @DisplayName("then invalid requests are rejected")
    @Test
    void testInvalidRequest() throws Exception {
        mockMvc.perform(post("/openid4vci/notification")
                        .contentType(MediaType.APPLICATION_JSON)
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

}
