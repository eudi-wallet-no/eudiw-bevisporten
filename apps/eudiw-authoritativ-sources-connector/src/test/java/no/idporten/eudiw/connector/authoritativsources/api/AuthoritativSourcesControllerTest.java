package no.idporten.eudiw.connector.authoritativsources.api;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
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
    @DisplayName("then Internal Server Error should be returned for unknown source")
    void unknownSource() {
        String source = "unknown";
        Exception exception = assertThrows(ServletException.class, () -> {
            mockMvc.perform(post("/api/v1/{source}/credentialdata/retrieve", source)
                    .content("""
                            {
                              "subject" : {
                                "identifier" : "50917500484"
                              }
                            }
                            """)
                    .contentType(MediaType.APPLICATION_JSON_VALUE));

        });

        assertTrue(exception.getMessage().contains("No AuthoritativeSource found for source %s".formatted(source)));
    }
}
