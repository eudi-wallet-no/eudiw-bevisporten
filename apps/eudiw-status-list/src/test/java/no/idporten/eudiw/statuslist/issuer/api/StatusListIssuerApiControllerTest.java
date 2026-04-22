package no.idporten.eudiw.statuslist.issuer.api;

import no.idporten.eudiw.statuslist.issuer.config.StatusIssuerProperties;
import no.idporten.eudiw.statuslist.service.Status;
import no.idporten.eudiw.statuslist.service.StatusListService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.List;

import static no.idporten.eudiw.statuslist.exceptions.ErrorCodes.INVALID_REQUEST;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("When using a AdminApi")
@ActiveProfiles("junit")
@AutoConfigureMockMvc
@SpringBootTest
class StatusListIssuerApiControllerTest {

    public static final String PATH = "/status-issuer/api/v1/entries";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StatusIssuerProperties statusIssuerProperties;

    @MockitoSpyBean
    private StatusListService statuslistService;

    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("then a POST with a valid StatusRequest will return a StatusResponse with the expected number of status entries")
    @Test
    void allocateStatus() throws Exception {
        int number = 5;
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-KEY", statusIssuerProperties.apiKey())
                        .content("{\"number_of_entries\":" + number + "}"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").exists())
                .andExpect(jsonPath("$.status_list_entries").isArray())
                .andExpect(jsonPath("$.status_list_entries", hasSize(number)))
                .andExpect(jsonPath("$.status_list_entries[0].idx").value(0))
                .andExpect(jsonPath("$.status_list_entries[0].uri").exists());
        verify(statuslistService).allocateToStatusList(eq(number));
    }

    @DisplayName("then a POST with a valid StatusRequest but no api-key will 401")
    @Test
    void allocateStatusFailsWithoutApiKey() throws Exception {
        int number = 5;
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"number_of_entries\":" + number + "}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$").exists())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error").value(INVALID_REQUEST));
        verify(statuslistService, never()).allocateToStatusList(anyInt());
    }

    @DisplayName("then a POST with a invalid StatusRequest will return 400 bad request")
    @Test
    void allocateStatusWithEmptyInputGivesBadRequest() throws Exception {
        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-KEY", statusIssuerProperties.apiKey())
                        .content(""))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").exists())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error").value(INVALID_REQUEST));
        verify(statuslistService, never()).allocateToStatusList(anyInt());
    }


    @DisplayName("then a PUT with a invalid StatusRequest will return 400 bad request")
    @Test
    void revokeStatusWithEmptyInputGivesBadRequest() throws Exception {
        mockMvc.perform(put(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-KEY", statusIssuerProperties.apiKey())
                        .content(""))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").exists())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error").value(INVALID_REQUEST));
        verify(statuslistService, never()).updateStatus(anyInt(),anyInt());
    }

    @DisplayName("then a PUT with a valid StatusUpdateRequest will return a 204 No Content response")
    @Test
    void revokeStatus() throws Exception {

        int idx = 19;
        StatusEntryUpdateRequest statusToRevoke = new StatusEntryUpdateRequest(idx, URI.create("https://junit.eidas2sandkasse.dev/1"), "INVALID");

        String statusesToRevoke = objectMapper.writeValueAsString(new StatusUpdateRequest(List.of(statusToRevoke)));
        mockMvc.perform(put(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-KEY", statusIssuerProperties.apiKey())
                        .content(statusesToRevoke))
                .andExpect(status().isNoContent());
        verify(statuslistService, times(1)).updateStatus(eq(idx),eq(Status.INVALID));
    }
}
