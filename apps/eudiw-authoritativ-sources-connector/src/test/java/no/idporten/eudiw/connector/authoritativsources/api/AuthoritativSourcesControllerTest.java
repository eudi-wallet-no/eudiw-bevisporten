package no.idporten.eudiw.connector.authoritativsources.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthoritativSourcesController.class)
@DisplayName("When retrieving credential data from a source ")
public class AuthoritativSourcesControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("then credential data for given source is returned")
    void apiTest() throws Exception {
        mockMvc.perform(post("/api/v1/{source}/credentialdata/retrieve", "123")
                        .content("""
                                {
                                  "subject" : {
                                    "identifier" : "50917500484"
                                  }
                                }
                                """)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credential_data.source").value("123"));
    }
}
