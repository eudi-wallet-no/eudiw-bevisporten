package no.idporten.eudiw.issuer.api.no.idporten.eudiw.issuer.api;


import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("When using the credential endpoint")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
public class CredentialEndpointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("then a valid credential request gives a credential response")
    @Test
    void testPostCredentialRequest() throws Exception {
        mockMvc.perform(post("/openid4vci/credential")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                                {
                                    "credential_configuration_id": "no.digdir.eudiw.pid_mso_mdoc"
                                }"""))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.credentials").isArray())
                .andExpect(jsonPath("$.credentials").isNotEmpty())
                .andExpect(jsonPath("$.credentials[0].credential").value(Matchers.containsString("personal_administrative_number")));
    }

    @DisplayName("then an empty credential request gives a credential error response")
    @Test
    void testEmptyCredentialRequest() throws Exception {
        mockMvc.perform(post("/openid4vci/credential")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(" {}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.error").value("invalid_credential_request"));
    }


    @DisplayName("then a credential request can not have both credential_identifier and credential_configuration_id")
    @Test
    void testEitherCredentialIdentifierOrCredentialConfigurationId() throws Exception {
        mockMvc.perform(post("/openid4vci/credential")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content("""
                                {
                                    "credential_identifier": "ci",
                                    "credential_configuration_id": "cci"
                                }"""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.error").value("invalid_credential_request"));
    }

}
