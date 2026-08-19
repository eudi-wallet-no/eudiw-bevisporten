package no.idporten.eudiw.issuer.ui.web;

import no.idporten.eudiw.issuer.ui.issuer.IssuerServerService;
import no.idporten.eudiw.issuer.ui.issuer.config.CredentialConfiguration;
import no.idporten.eudiw.issuer.ui.issuer.domain.AuthorizedCodeGrant;
import no.idporten.eudiw.issuer.ui.issuer.domain.CredentialOffer;
import no.idporten.eudiw.issuer.ui.issuer.domain.Grants;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.Collections;

import static no.idporten.eudiw.issuer.ui.web.StartIssuanceController.VIEW_ISSUANCE_RESPONSE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Test endpoints ")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
class StartIssuanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean
    private IssuerServerService issuerServerService;

    @DisplayName("start-issuance will returns request and issuance attributes")
    @Test
    void testStartIssuance() throws Exception {

        String configuration_id = "test-id";
        String issuerServer = "issuer-server";
        CredentialOffer offer = new CredentialOffer(issuerServer, Collections.singletonList(configuration_id), new Grants(new AuthorizedCodeGrant(null, null)));
        doReturn(offer).when(issuerServerService).startIssuance(any(CredentialConfiguration.class), any(URI.class));
        mockMvc.perform(get("/start-issuance?credential_configuration_id={configuration_id}", configuration_id))
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_ISSUANCE_RESPONSE))
                .andExpect(model().attributeExists("request"))
                .andExpect(model().attribute("request", Matchers.notNullValue()))
//                .andExpect(model().attribute("request", hasProperty("uri")))
                .andExpect(model().attributeExists("issuance"))
                .andExpect(model().attribute("issuance", Matchers.notNullValue()))
//                .andExpect(model().attribute("issuance", hasProperty("encodedUri", Matchers.notNullValue())))
//                .andExpect(model().attribute("issuance", hasProperty("qrCode", Matchers.notNullValue())))
//                .andExpect(model().attribute("issuance", hasProperty("issuanceResponse", Matchers.notNullValue())))
        ;
    }
}