package no.idporten.eudiw.statuslist.provider;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.statuslist.exceptions.StatusListNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatusProviderController.class)
public class StatusProviderControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatusProviderService statusProviderService;

    @BeforeEach
    public void setup() {
        JWT jwt = new PlainJWT(new JWTClaimsSet.Builder()
                .subject("https://status.eidas2sandkasse.dev/lists/1")
                .claim("ttl", 43200L)
                .claim("status_list", Map.of(
                        "bits", 1L,
                        "lst", "eNrbuRgAAhcBXQ"
                ))
                .build());

        when(statusProviderService.getStatusList(any())).thenReturn(jwt);
    }

    @Test
    @DisplayName("Should return a valid JWT with status list claims")
    void getStatusListClaims() throws Exception {
        MvcResult result = mockMvc.perform(get("/lists/{id}", 1).accept("application/statuslist+jwt"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/statuslist+jwt"))
                .andReturn();

        String jwtString = result.getResponse().getContentAsString();

        PlainJWT jwt = PlainJWT.parse(jwtString);
        JWTClaimsSet claims = jwt.getJWTClaimsSet();

        assertEquals("https://status.eidas2sandkasse.dev/lists/1", claims.getSubject());
        assertEquals(43200L, claims.getClaim("ttl"));

        Map<String, Object> statusList = claims.getJSONObjectClaim("status_list");
        assertEquals(1L, statusList.get("bits"));
        assertEquals("eNrbuRgAAhcBXQ", statusList.get("lst"));
    }

    @Test
    @DisplayName("Should return 404 when calling /non-existing-service")
    void getNoSuchServiceFound() throws Exception {
        mockMvc.perform(get("/non-existing-service").accept("application/statuslist+jwt"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.error_description").value(containsString("Requested resource not found")));
    }

    @Test
    @DisplayName("Should return 404 when calling non-existing id")
    void statusListNotFound() throws Exception {
        String invalidId = "-1";
        when(statusProviderService.getStatusList(invalidId)).thenThrow(new StatusListNotFoundException(invalidId));

        mockMvc.perform(get("/lists/{id}", invalidId).accept("application/statuslist+jwt"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("status_list_not_found"))
                .andExpect(jsonPath("$.error_description").value(containsString("Could not find status list with id %s".formatted(invalidId))));
    }
}
