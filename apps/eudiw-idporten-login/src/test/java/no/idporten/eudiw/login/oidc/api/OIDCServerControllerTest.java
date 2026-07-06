package no.idporten.eudiw.login.oidc.api;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("junit")
@AutoConfigureMockMvc
@SpringBootTest
public class OIDCServerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("When using OIDC discovery to explore metadata")
    @Nested
    class MetadataTests {

        @DisplayName("then metadata contains acr values for EU and Norwegian logins")
        @Test
        void metadataContainsAcrValuesForEUAndNorwegianLogins() throws Exception {
            mockMvc.perform(get("/.well-known/openid-configuration"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.acr_values_supported", hasItems(
                            "idporten-loa-substantial",
                            "idporten-loa-high",
                            "eidas-loa-substantial",
                            "eidas-loa-high")))
            .andExpect(jsonPath("$.acr_values_supported", hasSize(4)));
        }
    }

}
