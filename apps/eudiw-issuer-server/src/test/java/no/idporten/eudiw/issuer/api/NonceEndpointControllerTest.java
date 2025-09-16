package no.idporten.eudiw.issuer.api;


import com.nimbusds.openid.connect.sdk.Nonce;
import no.idporten.eudiw.issuer.openid4vci.service.NonceService;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("When using the nonce endpoint")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
public class NonceEndpointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NonceService nonceService;

    @MockitoBean
    private AuditLogger auditLogger;

    @DisplayName("then a new nonce is returned for a nonce request")
    @Test
    void testPostNonceRequest() throws Exception {
        when(nonceService.generateNonce()).thenReturn(new Nonce("foo"));
        mockMvc.perform(post("/openid4vci/nonce"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.c_nonce").value("foo"));
    }

}
