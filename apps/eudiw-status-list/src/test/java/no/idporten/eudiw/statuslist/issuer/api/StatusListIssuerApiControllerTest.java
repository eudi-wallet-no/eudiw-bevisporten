package no.idporten.eudiw.statuslist.issuer.api;

import no.idporten.eudiw.statuslist.issuer.config.StatusIssuerProperties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("When using a AdminApi")
@ActiveProfiles("junit")
@AutoConfigureMockMvc
@SpringBootTest
class StatusListIssuerApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StatusIssuerProperties statusIssuerProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("then a POST with a valid StatusRequest will return a StatusResponse with the expected number of status entries")
    @Test
    void allocateStatus() throws Exception {
        String path = "/status-issuer/api/v1/entries";
        int number = 5;
        mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-KEY", statusIssuerProperties.apiKey())
                        .content("{\"number_of_entries\":" + number + "}"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").exists())
                .andExpect(jsonPath("$.status_list_entries").isArray())
                .andExpect(jsonPath("$.status_list_entries", hasSize(number)))
                .andExpect(jsonPath("$.status_list_entries[0].idx").value(0))
                .andExpect(jsonPath("$.status_list_entries[0].uri").value("https://status.eidas2sandkasse.dev/1"));
    }

    @DisplayName("then a POST with a valid StatusRequest but no api-key will 401")
    @Test
    void allocateStatusFailsWithoutApiKey() throws Exception {
        String path = "/status-issuer/api/v1/entries";
        int number = 5;
        mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"number_of_entries\":" + number + "}"))
                .andExpect(status().isUnauthorized());

    }

    @Disabled // add error handling first
    @DisplayName("then a POST with a invalid StatusRequest will return 400 bad request")
    @Test
    void allocateStatusWithEmptyInputGivesBadRequest() throws Exception {
        String path = "/status-issuer/api/v1/entries";
        mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-KEY", statusIssuerProperties.apiKey())
                        .content(""))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("then a PUT with a valid StatusUpdateRequest will return a 204 No Content response")
    @Test
    void revokeStatus() throws Exception {
        String path = "/status-issuer/api/v1/entries";

        StatusEntryUpdateRequest statusToRevoke = new StatusEntryUpdateRequest(19, "https://junit.eidas2sandkasse.dev/1", "INVALID");

        String statusesToRevoke = objectMapper.writeValueAsString(new StatusUpdateRequest(List.of(statusToRevoke)));
        mockMvc.perform(put(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-KEY", statusIssuerProperties.apiKey())
                        .content(statusesToRevoke))
                .andExpect(status().isNoContent());
    }
}
