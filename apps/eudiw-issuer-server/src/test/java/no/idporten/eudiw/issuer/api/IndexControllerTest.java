package no.idporten.eudiw.issuer.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@DisplayName("When accessing the root path")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
public class IndexControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("then redirect to the credential issuer metadata endpoint")
    @Test
    void testRedirectRootToMetadata() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/.well-known/openid-credential-issuer"));
    }

}
