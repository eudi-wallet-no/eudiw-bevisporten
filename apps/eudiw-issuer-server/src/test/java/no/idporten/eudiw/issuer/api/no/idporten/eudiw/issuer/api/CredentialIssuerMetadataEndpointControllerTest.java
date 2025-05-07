package no.idporten.eudiw.issuer.api.no.idporten.eudiw.issuer.api;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("When accessing the credential issuer metadata endpoint")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
public class CredentialIssuerMetadataEndpointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("then the issuers metadata is returned in a JSON format")
    @Test
    void testGetMetadata() throws Exception {
        mockMvc.perform(get("/.well-known/openid-credential-issuer"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.credential_issuer").value("https://junit.eidas2sandkasse.dev/"))
                .andExpect(jsonPath("$.credential_endpoint").value("https://junit.eidas2sandkasse.dev/credential"));
    }

}
