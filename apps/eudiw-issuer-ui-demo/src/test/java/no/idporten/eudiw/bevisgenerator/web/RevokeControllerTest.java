package no.idporten.eudiw.bevisgenerator.web;

import no.idporten.eudiw.bevisgenerator.exception.IssuerServerException;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.IssuerServerService;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.CredentialConfiguration;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.IssuerServerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.view.InternalResourceView;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RevokeControllerTest {

    private MockMvc mockMvc;
    private IssuerServerService issuerServerService;
    private CredentialConfiguration credentialConfiguration;
    private CredentialConfiguration subjectCredentialConfiguration;

    @BeforeEach
    void setUp() {
        issuerServerService = mock(IssuerServerService.class);
        IssuerServerProperties issuerServerProperties = mock(IssuerServerProperties.class);

        credentialConfiguration = new CredentialConfiguration(
                "http://issuer",
                "no.digdir.eudiw.pid_mso_mdoc",
                "scope",
                "12345678910",
                "PID",
                "{\"a\":\"b\"}"
        );
        subjectCredentialConfiguration = new CredentialConfiguration(
                "http://issuer",
                "proof_of_age",
                "proof_of_age",
                null,
                "Aldersbevis",
                null
        );

        when(issuerServerProperties.credentialIssuer()).thenReturn("http://issuer");
        when(issuerServerService.getAll()).thenReturn(List.of(credentialConfiguration));
        when(issuerServerService.getAllSubjectCredentialConfigurations()).thenReturn(List.of(subjectCredentialConfiguration));

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new RevokeController(issuerServerService, issuerServerProperties))
                .setValidator(validator)
                .setViewResolvers((viewName, locale) -> {
                    InternalResourceView view = new InternalResourceView();
                    view.setUrl("/templates/" + viewName + ".html");
                    return view;
                })
                .build();
    }

    @Test
    void getRevokePageReturnsViewWithModel() throws Exception {
        mockMvc.perform(get("/revoke"))
                .andExpect(status().isOk())
                .andExpect(view().name("revoke"))
                .andExpect(model().attributeExists("revokeForm"))
                .andExpect(model().attributeExists("revokeBySubjectForm"))
                .andExpect(model().attribute("revocationMethod", ""))
                .andExpect(model().attributeExists("credentialConfigurations"))
                .andExpect(model().attributeExists("subjectCredentialConfigurations"));
    }

    @Test
    void revokePageRendersMethodPickerDynamicNavigationAndProcessedAlert() throws Exception {
        org.springframework.web.context.support.GenericWebApplicationContext applicationContext =
                new org.springframework.web.context.support.GenericWebApplicationContext();
        applicationContext.refresh();

        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        templateResolver.setApplicationContext(applicationContext);
        templateResolver.setPrefix("classpath:/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(org.thymeleaf.templatemode.TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine);
        viewResolver.setCharacterEncoding("UTF-8");

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        IssuerServerProperties issuerServerProperties = mock(IssuerServerProperties.class);
        when(issuerServerProperties.credentialIssuer()).thenReturn("http://issuer");

        MockMvc thymeleafMockMvc = MockMvcBuilders.standaloneSetup(
                        new RevokeController(issuerServerService, issuerServerProperties))
                .setValidator(validator)
                .setViewResolvers(viewResolver)
                .build();

        thymeleafMockMvc.perform(get("/revoke"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"step-flow\"")))
                .andExpect(content().string(containsString("class=\"step-flow__content\"")))
                .andExpect(content().string(containsString("class=\"ds-fieldset revocation-methods\"")))
                .andExpect(content().string(containsString("data-variant=\"outline\"")))
                .andExpect(content().string(containsString("data-clickdelegatefor=\"revocation-method-transaction-id\"")))
                .andExpect(content().string(containsString("data-clickdelegatefor=\"revocation-method-person-identifier\"")))
                .andExpect(content().string(containsString("x-model=\"selectedForm\"")))
                .andExpect(content().string(containsString("x-bind:form=\"selectedForm\"")))
                .andExpect(content().string(containsString("x-bind:disabled=\"!selectedForm\"")))
                .andExpect(content().string(not(containsString("data-color=\"danger\""))))
                .andExpect(content().string(containsString("Tilbakekall utstedte bevis")))
                .andExpect(content().string(containsString("Tilbakekall bevis")))
                .andExpect(content().string(containsString("Du må først velje korleis du vil finne beviset.")))
                .andExpect(content().string(containsString("Fyll inn opplysningane")))
                .andExpect(content().string(not(containsString("Vel opplysninga du har tilgjengeleg."))));

        thymeleafMockMvc.perform(post("/revoke")
                        .param("credentialConfigurationId", "")
                        .param("issuanceTransactionId", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("selectedForm: &#39;revoke-form&#39;")))
                .andExpect(content().string(containsString("Skriv inn transaksjons-ID-en")));

        thymeleafMockMvc.perform(post("/revoke/by-subject")
                        .param("credentialConfigurationId", "")
                        .param("subjectIdentifier", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("selectedForm: &#39;revoke-by-subject-form&#39;")))
                .andExpect(content().string(containsString("Skriv inn personidentifikatoren")));

        when(issuerServerService.getById(credentialConfiguration.credentialConfigurationId()))
                .thenReturn(credentialConfiguration);

        thymeleafMockMvc.perform(post("/revoke")
                        .param("credentialConfigurationId", credentialConfiguration.credentialConfigurationId())
                        .param("issuanceTransactionId", "tx-123"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"ds-alert\"")))
                .andExpect(content().string(containsString("data-color=\"info\"")))
                .andExpect(content().string(containsString("Dersom beviset fanst, er det no tilbakekalla.")))
                .andExpect(content().string(containsString("href=\"/verification-start\"")))
                .andExpect(content().string(containsString("Presenter beviset på nytt")));
    }

    @Test
    void postRevokeMarksRevocationAsProcessed() throws Exception {
        when(issuerServerService.getById(credentialConfiguration.credentialConfigurationId())).thenReturn(credentialConfiguration);

        mockMvc.perform(post("/revoke")
                        .param("credentialConfigurationId", credentialConfiguration.credentialConfigurationId())
                        .param("issuanceTransactionId", "tx-123"))
                .andExpect(status().isOk())
                .andExpect(view().name("revoke"))
                .andExpect(model().attribute("revocationMethod", RevokeController.TRANSACTION_ID_METHOD))
                .andExpect(model().attribute("txIdRevocationProcessed", true));

        verify(issuerServerService).revokeCredential(eq(credentialConfiguration), eq("tx-123"));
    }

    @Test
    void postRevokeReturnsBadRequestForInvalidInput() throws Exception {
        mockMvc.perform(post("/revoke")
                        .param("credentialConfigurationId", "")
                        .param("issuanceTransactionId", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("revoke"))
                .andExpect(model().attribute("revocationMethod", RevokeController.TRANSACTION_ID_METHOD))
                .andExpect(model().attributeHasFieldErrors("revokeForm", "credentialConfigurationId", "issuanceTransactionId"));
    }

    @Test
    void postRevokeReturnsBadGatewayWhenIssuerServerFails() throws Exception {
        when(issuerServerService.getById(credentialConfiguration.credentialConfigurationId())).thenReturn(credentialConfiguration);

        HttpClientErrorException cause = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                null,
                new byte[0],
                null
        );
        doThrow(new IssuerServerException("revoke failed", cause))
                .when(issuerServerService)
                .revokeCredential(eq(credentialConfiguration), anyString());

        mockMvc.perform(post("/revoke")
                        .param("credentialConfigurationId", credentialConfiguration.credentialConfigurationId())
                        .param("issuanceTransactionId", "tx-123"))
                .andExpect(status().isOk())
                .andExpect(view().name("revoke"))
                .andExpect(model().attribute("revocationMethod", RevokeController.TRANSACTION_ID_METHOD))
                .andExpect(model().attributeExists("txIdErrorMessage"));
    }

    @Test
    void postRevokeBySubjectMarksRevocationAsProcessedWithArbitrarySubjectInput() throws Exception {
        when(issuerServerService.getSubjectCredentialConfigurationById(subjectCredentialConfiguration.credentialConfigurationId()))
                .thenReturn(subjectCredentialConfiguration);

        mockMvc.perform(post("/revoke/by-subject")
                        .param("credentialConfigurationId", subjectCredentialConfiguration.credentialConfigurationId())
                        .param("subjectIdentifier", "abc-123-anything"))
                .andExpect(status().isOk())
                .andExpect(view().name("revoke"))
                .andExpect(model().attribute("revocationMethod", RevokeController.PERSON_IDENTIFIER_METHOD))
                .andExpect(model().attribute("subjectRevocationProcessed", true));

        verify(issuerServerService).revokeCredentialBySubject(eq(subjectCredentialConfiguration), eq("abc-123-anything"));
    }

    @Test
    void postRevokeBySubjectReturnsErrorWhenCredentialConfigurationIsUnknown() throws Exception {
        when(issuerServerService.getSubjectCredentialConfigurationById("unknown")).thenReturn(null);

        mockMvc.perform(post("/revoke/by-subject")
                        .param("credentialConfigurationId", "unknown")
                        .param("subjectIdentifier", "some-subject"))
                .andExpect(status().isOk())
                .andExpect(view().name("revoke"))
                .andExpect(model().attribute("revocationMethod", RevokeController.PERSON_IDENTIFIER_METHOD))
                .andExpect(model().attribute("subjectErrorMessage", "Bevistypen finst ikkje"));
    }

    @Test
    void postRevokeBySubjectPreservesMethodForInvalidInput() throws Exception {
        mockMvc.perform(post("/revoke/by-subject")
                        .param("credentialConfigurationId", "")
                        .param("subjectIdentifier", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("revoke"))
                .andExpect(model().attribute("revocationMethod", RevokeController.PERSON_IDENTIFIER_METHOD))
                .andExpect(model().attributeHasFieldErrors(
                        "revokeBySubjectForm",
                        "credentialConfigurationId",
                        "subjectIdentifier"
                ));
    }

    @Test
    void postRevokeBySubjectReturnsErrorWhenIssuerServerFails() throws Exception {
        when(issuerServerService.getSubjectCredentialConfigurationById(subjectCredentialConfiguration.credentialConfigurationId()))
                .thenReturn(subjectCredentialConfiguration);

        HttpClientErrorException cause = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                null,
                new byte[0],
                null
        );
        doThrow(new IssuerServerException("revoke by subject failed", cause))
                .when(issuerServerService)
                .revokeCredentialBySubject(eq(subjectCredentialConfiguration), anyString());

        mockMvc.perform(post("/revoke/by-subject")
                        .param("credentialConfigurationId", subjectCredentialConfiguration.credentialConfigurationId())
                        .param("subjectIdentifier", "05821098825"))
                .andExpect(status().isOk())
                .andExpect(view().name("revoke"))
                .andExpect(model().attribute("revocationMethod", RevokeController.PERSON_IDENTIFIER_METHOD))
                .andExpect(model().attributeExists("subjectErrorMessage"));
    }

}
