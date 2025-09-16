package no.idporten.eudiw.issuer.api;


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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("When creating credential offers")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
public class CredentialOfferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogger auditLogger;

    @DisplayName("then a credential offer can be created for a credential configuration that uses the authorization code flow")
    @Test
    void testCreateCredentialOfferForAuthorizationCodeFlow() throws Exception {
        mockMvc.perform(get("/api/v1/credential-offer/create")
                        .queryParam("credential_configuration_id", "junitdoc_mso_mdoc")
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.credential_issuer").value("https://junit.eidas2sandkasse.dev/"))
                .andExpect(jsonPath("$.credential_configuration_ids").isArray())
                .andExpect(jsonPath("$.credential_configuration_ids[0]").value("junitdoc_mso_mdoc"))
                .andExpect(jsonPath("$.grants.authorization_code").exists())
                .andExpect(jsonPath("$.grants.authorization_code").isEmpty());
    }

    @DisplayName("then a credential offer cannot be created for a credential configuration that uses the pre-authorized code flow")
    @Test
    void testNoCredentialOfferForPreAuthorizedCodeFlow() throws Exception {
        mockMvc.perform(get("/api/v1/credential-offer/create")
                        .queryParam("credential_configuration_id", "junitdoc_pre_mso_mdoc")
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.error_description").value(Matchers.containsString("cannot be used with the authorization code flow")));
    }

    @DisplayName("then a credential offer cannot be created for an unknown credential configuration")
    @Test
    void testNoCredentialOfferForUnkownCredentialConfiguration() throws Exception {
        mockMvc.perform(get("/api/v1/credential-offer/create")
                        .queryParam("credential_configuration_id", "banana_mso_mdoc")
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.error").value("unknown_credential_identifier"));
    }

}
