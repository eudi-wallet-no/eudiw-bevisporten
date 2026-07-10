package no.idporten.eudiw.bevisgenerator.web;

import no.idporten.eudiw.bevisgenerator.integration.issuerserver.IssuerServerService;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.CredentialConfiguration;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.IssuerServerProperties;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.VerifierService;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerificationResult;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerificationStartResponse;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerificationTransactionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.servlet.view.RedirectView;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.Map;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VerificationControllerTest {

    private MockMvc mockMvc;
    private VerifierService verifierService;
    private CredentialConfiguration issuanceConfig;
    private CredentialConfiguration subjectConfig;

    @BeforeEach
    void setUp() {
        IssuerServerService issuerServerService = mock(IssuerServerService.class);
        IssuerServerProperties issuerServerProperties = mock(IssuerServerProperties.class);
        verifierService = mock(VerifierService.class);
        ObjectMapper objectMapper = new ObjectMapper();

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
        when(verifierService.startVerification(anyString())).thenReturn(
                new VerificationTransactionData(
                new VerificationStartResponse("eudi-openid4vp://example", "data:image/png;base64,abc123", "tx-id"),
                        URI.create("http://verifier/status/tx-id"),
                        URI.create("http://verifier/result/tx-id")
                ));
        when(verifierService.retrieveVerificationResult("tx-id")).thenReturn(new VerificationResult(
                "tx-id",
                Map.of(
                        "proof_of_age",
                        List.of(new VerificationResult.CredentialPresentation(
                                Map.of("age_over_18", true)
                        ))
                )
        ));

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new VerificationController(issuerServerService, issuerServerProperties, verifierService, objectMapper))
                .setValidator(validator)
                .setViewResolvers((viewName, locale) -> {
                    if (viewName.startsWith("redirect:")) {
                        return new RedirectView(viewName.substring("redirect:".length()));
                    }
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
    void postVerificationStartWithValidInputRedirectsToPresentation() throws Exception {
        mockMvc.perform(post("/verification-start")
                        .param("credentialConfigurationId", issuanceConfig.credentialConfigurationId())
                        .param("dcql", "{\"credentials\":[{\"id\":\"pid\"}]}"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/verification-presentation"))
                .andExpect(flash().attributeExists("qrCode"))
                .andExpect(flash().attributeExists("authorizationRequest"))
                .andExpect(flash().attribute("transactionId", "tx-id"));
    }

    @Test
    void getVerificationPresentationReturnsPresentationView() throws Exception {
        mockMvc.perform(get("/verification-presentation"))
                .andExpect(status().isOk())
                .andExpect(view().name("verification-presentation"));
    }

    @Test
    void getVerificationResultAddsResultAndPrettyJsonToModel() throws Exception {
        mockMvc.perform(get("/verification-result").param("transactionId", "tx-id"))
                .andExpect(status().isOk())
                .andExpect(view().name("verification-result"))
                .andExpect(model().attributeExists("result"))
                .andExpect(model().attribute("resultJson", containsString("\"proof_of_age\"")))
                .andExpect(model().attribute("resultJson", containsString("\"age_over_18\" : true")));

        verify(verifierService).retrieveVerificationResult("tx-id");
    }

    @Test
    void postVerificationStartWithBlankCredentialConfigurationIdFailsValidation() throws Exception {
        mockMvc.perform(post("/verification-start")
                        .param("credentialConfigurationId", "")
                        .param("dcql", "{\"credentials\":[{\"id\":\"pid\"}]}"))
                .andExpect(status().isOk())
                .andExpect(view().name("verification-start"))
                .andExpect(model().attributeHasFieldErrors("verificationForm", "credentialConfigurationId"))
                .andExpect(model().attributeDoesNotExist("verificationSuccessMessage"));
    }

    @Test
    void postVerificationStartWithBlankDcqlFailsValidation() throws Exception {
        mockMvc.perform(post("/verification-start")
                        .param("credentialConfigurationId", issuanceConfig.credentialConfigurationId())
                        .param("dcql", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("verification-start"))
                .andExpect(model().attributeHasFieldErrors("verificationForm", "dcql"))
                .andExpect(model().attributeDoesNotExist("verificationSuccessMessage"));
    }

    @Test
    void postVerificationStartWithBothFieldsBlankFailsValidationOnBoth() throws Exception {
        mockMvc.perform(post("/verification-start")
                        .param("credentialConfigurationId", "")
                        .param("dcql", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("verification-start"))
                .andExpect(model().attributeHasFieldErrors("verificationForm", "credentialConfigurationId", "dcql"));
    }

    @Test
    void postVerificationStartReturnsCredentialConfigurationsOnValidationError() throws Exception {
        mockMvc.perform(post("/verification-start")
                        .param("credentialConfigurationId", "")
                        .param("dcql", ""))
                .andExpect(status().isOk())
                .andExpect(model().attribute("credentialConfigurations", hasSize(2)));
    }
}
