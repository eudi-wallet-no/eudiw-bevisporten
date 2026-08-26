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
import org.springframework.web.context.support.GenericWebApplicationContext;
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
    private IssuerServerProperties issuerServerProperties;
    private CredentialConfiguration credentialConfiguration;
    private CredentialConfiguration subjectCredentialConfiguration;

    @BeforeEach
    void setUp() {
        issuerServerService = mock(IssuerServerService.class);
        issuerServerProperties = mock(IssuerServerProperties.class);

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
    void getRevokePageRendersMethodSelectionAndCredentialPicker() throws Exception {
        thymeleafMockMvc().perform(get("/revoke"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("x-data=\"revocationPage()\"")))
                .andExpect(content().string(containsString("Utferda bevis i denne nettlesaren")))
                .andExpect(content().string(containsString("Vel eit utferda bevis")))
                .andExpect(content().string(containsString("href=\"/issue\"")))
                .andExpect(content().string(containsString("href=\"/verification-start\"")))
                .andExpect(content().string(containsString("aria-label=\"Meny\"")))
                .andExpect(content().string(containsString("aria-hidden=\"true\">Meny</span>")))
                .andExpect(content().string(containsString("<dialog")));
    }

    @Test
    void postRevokeRendersSuccessResultPage() throws Exception {
        when(issuerServerService.getById(credentialConfiguration.credentialConfigurationId())).thenReturn(credentialConfiguration);
        when(issuerServerService.revokeCredential(eq(credentialConfiguration), anyString())).thenReturn(1);

        thymeleafMockMvc().perform(post("/revoke")
                        .param("credentialConfigurationId", credentialConfiguration.credentialConfigurationId())
                        .param("issuanceTransactionId", "tx-123"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-color=\"success\"")))
                .andExpect(content().string(containsString("Beviset er tilbakekalla")))
                .andExpect(content().string(containsString("<span>Presenter bevis</span>")))
                .andExpect(content().string(containsString("x-data=\"revocationResultHistory()\"")));
    }

    @Test
    void postRevokeRendersWarningResultPageWhenNothingMatched() throws Exception {
        when(issuerServerService.getById(credentialConfiguration.credentialConfigurationId())).thenReturn(credentialConfiguration);
        when(issuerServerService.revokeCredential(eq(credentialConfiguration), anyString())).thenReturn(0);

        thymeleafMockMvc().perform(post("/revoke")
                        .param("credentialConfigurationId", credentialConfiguration.credentialConfigurationId())
                        .param("issuanceTransactionId", "tx-unknown"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-color=\"warning\"")))
                .andExpect(content().string(containsString("Ingenting er tilbakekalla")))
                .andExpect(content().string(not(containsString("<span>Presenter bevis</span>"))));
    }

    private MockMvc thymeleafMockMvc() {
        GenericWebApplicationContext applicationContext = new GenericWebApplicationContext();
        applicationContext.refresh();

        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        templateResolver.setApplicationContext(applicationContext);
        templateResolver.setPrefix("classpath:/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine);
        viewResolver.setCharacterEncoding("UTF-8");

        return MockMvcBuilders.standaloneSetup(
                        new RevokeController(issuerServerService, issuerServerProperties))
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    void postRevokeShowsResultWhenCredentialWasRevoked() throws Exception {
        when(issuerServerService.getById(credentialConfiguration.credentialConfigurationId())).thenReturn(credentialConfiguration);
        when(issuerServerService.revokeCredential(eq(credentialConfiguration), eq("tx-123"))).thenReturn(1);

        mockMvc.perform(post("/revoke")
                        .param("credentialConfigurationId", credentialConfiguration.credentialConfigurationId())
                        .param("issuanceTransactionId", "tx-123"))
                .andExpect(status().isOk())
                .andExpect(view().name("revocation-result"))
                .andExpect(model().attribute("revokedCount", 1))
                .andExpect(model().attribute("credentialDescription", "PID"))
                .andExpect(model().attribute("processedIssuanceTransactionId", "tx-123"));

        verify(issuerServerService).revokeCredential(eq(credentialConfiguration), eq("tx-123"));
    }

    @Test
    void postRevokeShowsWarningAndClearsLocalEntryWhenNothingMatched() throws Exception {
        when(issuerServerService.getById(credentialConfiguration.credentialConfigurationId())).thenReturn(credentialConfiguration);
        when(issuerServerService.revokeCredential(eq(credentialConfiguration), eq("tx-unknown"))).thenReturn(0);

        mockMvc.perform(post("/revoke")
                        .param("credentialConfigurationId", credentialConfiguration.credentialConfigurationId())
                        .param("issuanceTransactionId", "tx-unknown"))
                .andExpect(status().isOk())
                .andExpect(view().name("revocation-result"))
                .andExpect(model().attribute("revokedCount", 0))
                .andExpect(model().attribute("processedIssuanceTransactionId", "tx-unknown"));
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
    void postRevokeBySubjectShowsResultWhenCredentialsWereRevoked() throws Exception {
        when(issuerServerService.getSubjectCredentialConfigurationById(subjectCredentialConfiguration.credentialConfigurationId()))
                .thenReturn(subjectCredentialConfiguration);
        when(issuerServerService.revokeCredentialBySubject(eq(subjectCredentialConfiguration), eq("abc-123-anything")))
                .thenReturn(2);

        mockMvc.perform(post("/revoke/by-subject")
                        .param("credentialConfigurationId", subjectCredentialConfiguration.credentialConfigurationId())
                        .param("subjectIdentifier", "abc-123-anything"))
                .andExpect(status().isOk())
                .andExpect(view().name("revocation-result"))
                .andExpect(model().attribute("revokedCount", 2))
                .andExpect(model().attribute("credentialDescription", "Aldersbevis"))
                .andExpect(model().attribute(
                        "processedCredentialConfigurationId",
                        subjectCredentialConfiguration.credentialConfigurationId()))
                .andExpect(model().attribute("processedSubjectIdentifier", "abc-123-anything"));

        verify(issuerServerService).revokeCredentialBySubject(eq(subjectCredentialConfiguration), eq("abc-123-anything"));
    }

    @Test
    void postRevokeBySubjectShowsWarningAndClearsLocalEntriesWhenNothingMatched() throws Exception {
        when(issuerServerService.getSubjectCredentialConfigurationById(subjectCredentialConfiguration.credentialConfigurationId()))
                .thenReturn(subjectCredentialConfiguration);
        when(issuerServerService.revokeCredentialBySubject(eq(subjectCredentialConfiguration), anyString()))
                .thenReturn(0);

        mockMvc.perform(post("/revoke/by-subject")
                        .param("credentialConfigurationId", subjectCredentialConfiguration.credentialConfigurationId())
                        .param("subjectIdentifier", "abc-123-anything"))
                .andExpect(status().isOk())
                .andExpect(view().name("revocation-result"))
                .andExpect(model().attribute("revokedCount", 0))
                .andExpect(model().attribute(
                        "processedCredentialConfigurationId",
                        subjectCredentialConfiguration.credentialConfigurationId()))
                .andExpect(model().attribute("processedSubjectIdentifier", "abc-123-anything"));
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
