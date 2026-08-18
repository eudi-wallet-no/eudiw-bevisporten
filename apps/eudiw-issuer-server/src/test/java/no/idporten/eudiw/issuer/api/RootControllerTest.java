package no.idporten.eudiw.issuer.api;

import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenantService;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@DisplayName("When accessing root paths on the credential issuer server")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
public class RootControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CredentialIssuerTenantService credentialIssuerTenantService;

    @MockitoBean
    private AuditLogger auditLogger;

    @DisplayName("then the / path will show page with information about all credential issuer tenants")
    @Test
    void testIndexPage() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"))
                .andReturn();
        String content = mvcResult.getResponse().getContentAsString();
        for (CredentialIssuerTenant credentialIssuerTenant : credentialIssuerTenantService.findAllTenants()) {
            assertTrue(content.contains(credentialIssuerTenant.getDisplayNames().get("no")));
        }
    }

    @DisplayName("then a request to a tenant credential issuer uri will redirect to the tenant's credential issuer metadata endpoint")
    @ParameterizedTest
    @ValueSource(strings = {"/junit", "/junit/"})
    void testRedirectTenantToMetadata(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/.well-known/openid-credential-issuer/junit"));
    }

}
