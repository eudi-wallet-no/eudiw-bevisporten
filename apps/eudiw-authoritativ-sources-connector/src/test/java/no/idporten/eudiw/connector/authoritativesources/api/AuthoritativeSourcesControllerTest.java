package no.idporten.eudiw.connector.authoritativesources.api;

import no.idporten.eudiw.connector.authoritativesources.TestData;
import no.idporten.eudiw.connector.authoritativesources.exceptions.ErrorCodes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("When retrieving credential data from a source ")
public class AuthoritativeSourcesControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("then credential data for given source is returned")
    void validDataTest() throws Exception {
        mockMvc.perform(post("/api/v1/{source}/credentialdata/retrieve", "junit")
                        .content("""
                                {
                                  "subject" : {
                                    "identifier" : "%s"
                                  }
                                }
                                """.formatted(TestData.getValidSyntheticPersonIdentifier()))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credential_data.identifier").value("50917500484"));
    }

    @Test
    @DisplayName("then 'invalid_request' and description should be returned for unknown source")
    void invalidSourceTest() throws Exception {
        String source = "unknown";
        mockMvc.perform(post("/api/v1/{source}/credentialdata/retrieve", source)
                        .content("""
                                {
                                  "subject" : {
                                    "identifier" : "%s"
                                  }
                                }
                                """.formatted(TestData.getValidSyntheticPersonIdentifier()))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(ErrorCodes.INVALID_REQUEST))
                .andExpect(jsonPath("$.error_description").value(containsString("Authoritative source not found")))
                .andExpect(jsonPath("$.error_description").value(containsString("trace_id")));
    }

    @Test
    @DisplayName("then 'invlaid_request' and description should be returned for invalid body")
    void invalidRequestDataTest() throws Exception {
        mockMvc.perform(post("/api/v1/{source}/credentialdata/retrieve", "junit")
                        .content("""
                                {
                                  "subject" : {
                                    "identifier" : "%s"
                                  }
                                }
                                """.formatted(TestData.getInvalidSyntheticPersonIdentifier()))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.error").value(ErrorCodes.INVALID_REQUEST))
                .andExpect(jsonPath("$.error_description").value(containsString("Invalid person identifier")))
                .andExpect(jsonPath("$.error_description").value(containsString("trace_id")));
    }
}
