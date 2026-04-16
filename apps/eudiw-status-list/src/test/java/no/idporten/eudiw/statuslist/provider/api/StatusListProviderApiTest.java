package no.idporten.eudiw.statuslist.provider.api;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatusListProviderController.class)
public class StatusListProviderApiTest {
    @Autowired
    private MockMvc mockMvc;

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
}
