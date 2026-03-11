package no.idporten.eudiw.connector.authoritativsources.api;

import no.idporten.eudiw.connector.authoritativsources.exceptions.ErrorCodes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("When retrieving credential data from a source ")
public class AuthoritativSourcesControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("then credential data for given source is returned")
    void apiTest() throws Exception {
        mockMvc.perform(post("/api/v1/{source}/credentialdata/retrieve", "junit")
                        .content("""
                                {
                                  "subject" : {
                                    "identifier" : "50917500484"
                                  }
                                }
                                """)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credential_data.identifier").value("50917500484"));
    }

    @Test
    @DisplayName("then Bad Request should be returned for unknown source")
    void exceptionTest() throws Exception {
        String source = "unknown";
        mockMvc.perform(post("/api/v1/{source}/credentialdata/retrieve", source)
                        .content("""
                                {
                                  "subject" : {
                                    "identifier" : "50917500484"
                                  }
                                }
                                """)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_description").value("No AuthoritativeSource found for given source"))
                .andExpect(jsonPath("$.error").value(ErrorCodes.INVALID_REQUEST));
    }
}
