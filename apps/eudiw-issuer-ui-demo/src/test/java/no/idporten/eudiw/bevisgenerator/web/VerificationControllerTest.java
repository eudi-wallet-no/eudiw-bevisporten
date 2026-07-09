package no.idporten.eudiw.bevisgenerator.web;

import no.idporten.eudiw.bevisgenerator.integration.issuerserver.IssuerServerService;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.CredentialConfiguration;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.IssuerServerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.view.InternalResourceView;

import java.util.List;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class VerificationControllerTest {

    private MockMvc mockMvc;
    private CredentialConfiguration issuanceConfig;
    private CredentialConfiguration subjectConfig;

    @BeforeEach
    void setUp() {
        IssuerServerService issuerServerService = mock(IssuerServerService.class);
        IssuerServerProperties issuerServerProperties = mock(IssuerServerProperties.class);

        issuanceConfig = new CredentialConfiguration(
                "http://issuer",
                "no.digdir.eudiw.pid_mso_mdoc",
                "scope",
                "12345678910",
                "PID",
                "{\"a\":\"b\"}"
        );
        subjectConfig = new CredentialConfiguration(
                "http://issuer",
                "proof_of_age",
                "proof_of_age",
                null,
                "Aldersbevis",
                null
        );

        when(issuerServerProperties.credentialIssuer()).thenReturn("http://issuer");
        when(issuerServerService.getAll()).thenReturn(List.of(issuanceConfig));
        when(issuerServerService.getAllSubjectCredentialConfigurations()).thenReturn(List.of(subjectConfig));

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new VerificationController(issuerServerService, issuerServerProperties))
                .setValidator(validator)
                .setViewResolvers((viewName, locale) -> {
                    InternalResourceView view = new InternalResourceView();
                    view.setUrl("/templates/" + viewName + ".html");
                    return view;
                })
                .build();
    }

    @Test
    void getVerificationStartReturnsViewWithEmptyForm() throws Exception {
        mockMvc.perform(get("/verification-start"))
                .andExpect(status().isOk())
                .andExpect(view().name("verification-start"))
                .andExpect(model().attributeExists("verificationForm"))
                .andExpect(model().attributeExists("credentialConfigurations"));
    }

    @Test
    void getVerificationStartCombinesBothCredentialConfigurationLists() throws Exception {
        mockMvc.perform(get("/verification-start"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("credentialConfigurations", hasSize(2)))
                .andExpect(model().attribute("credentialConfigurations",
                        hasItems(issuanceConfig, subjectConfig)));
    }

    @Test
    void postVerificationStartWithValidInputReturnsSuccessMessage() throws Exception {
        mockMvc.perform(post("/verification-start")
                        .param("credentialConfigurationId", issuanceConfig.credentialConfigurationId())
                        .param("dcqlQuery", "{\"credentials\":[{\"id\":\"pid\"}]}"))
                .andExpect(status().isOk())
                .andExpect(view().name("verification-start"))
                .andExpect(model().attribute("verificationSuccessMessage", "DCQL query er registrert."))
                .andExpect(model().attributeExists("credentialConfigurations"));
    }

    @Test
    void postVerificationStartWithBlankCredentialConfigurationIdFailsValidation() throws Exception {
        mockMvc.perform(post("/verification-start")
                        .param("credentialConfigurationId", "")
                        .param("dcqlQuery", "{\"credentials\":[{\"id\":\"pid\"}]}"))
                .andExpect(status().isOk())
                .andExpect(view().name("verification-start"))
                .andExpect(model().attributeHasFieldErrors("verificationForm", "credentialConfigurationId"))
                .andExpect(model().attributeDoesNotExist("verificationSuccessMessage"));
    }

    @Test
    void postVerificationStartWithBlankDcqlFailsValidation() throws Exception {
        mockMvc.perform(post("/verification-start")
                        .param("credentialConfigurationId", issuanceConfig.credentialConfigurationId())
                        .param("dcqlQuery", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("verification-start"))
                .andExpect(model().attributeHasFieldErrors("verificationForm", "dcqlQuery"))
                .andExpect(model().attributeDoesNotExist("verificationSuccessMessage"));
    }

    @Test
    void postVerificationStartWithBothFieldsBlankFailsValidationOnBoth() throws Exception {
        mockMvc.perform(post("/verification-start")
                        .param("credentialConfigurationId", "")
                        .param("dcqlQuery", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("verification-start"))
                .andExpect(model().attributeHasFieldErrors("verificationForm", "credentialConfigurationId", "dcqlQuery"));
    }

    @Test
    void postVerificationStartReturnsCredentialConfigurationsOnValidationError() throws Exception {
        mockMvc.perform(post("/verification-start")
                        .param("credentialConfigurationId", "")
                        .param("dcqlQuery", ""))
                .andExpect(status().isOk())
                .andExpect(model().attribute("credentialConfigurations", hasSize(2)));
    }
}
