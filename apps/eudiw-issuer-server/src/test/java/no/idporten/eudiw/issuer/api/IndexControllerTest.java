package no.idporten.eudiw.issuer.api;

import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@DisplayName("When accessing the root path")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
public class IndexControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogger auditLogger;

    @DisplayName("then show page with links to metadata and documentation endpoints")
    @Test
    void testIndexPage() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andReturn();
        String content = mvcResult.getResponse().getContentAsString();
        assertAll(
                () -> content.contains(Endpoints.METADATA_ENDPOINT),
                () -> content.contains(Endpoints.OPENAPI_ENDPOINT)
        );
    }

}
