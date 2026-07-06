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

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class RevokeControllerTest {

    private MockMvc mockMvc;
    private IssuerServerService issuerServerService;
    private CredentialConfiguration credentialConfiguration;

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

        when(issuerServerProperties.credentialIssuer()).thenReturn("http://issuer");
        when(issuerServerService.getAll()).thenReturn(List.of(credentialConfiguration));

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
                .andExpect(model().attributeExists("credentialConfigurations"));
    }

    @Test
    void postRevokeReturnsSuccessMessageWhenRevoked() throws Exception {
        when(issuerServerService.getById(credentialConfiguration.credentialConfigurationId())).thenReturn(credentialConfiguration);

        mockMvc.perform(post("/revoke")
                        .param("credentialConfigurationId", credentialConfiguration.credentialConfigurationId())
                        .param("issuanceTransactionId", "tx-123"))
                .andExpect(status().isOk())
                .andExpect(view().name("revoke"))
                .andExpect(model().attribute("txIdSuccessMessage", "Beviset er revokert dersom det eksisterte."));

        verify(issuerServerService).revokeCredential(eq(credentialConfiguration), eq("tx-123"));
    }

    @Test
    void postRevokeReturnsBadRequestForInvalidInput() throws Exception {
        mockMvc.perform(post("/revoke")
                        .param("credentialConfigurationId", "")
                        .param("issuanceTransactionId", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("revoke"))
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
                .andExpect(model().attributeExists("txIdErrorMessage"));
    }

    @Test
    void postRevokeBySubjectReturnsSuccessMessageWithArbitrarySubjectInput() throws Exception {
        when(issuerServerService.getById(credentialConfiguration.credentialConfigurationId())).thenReturn(credentialConfiguration);

        mockMvc.perform(post("/revoke/by-subject")
                        .param("credentialConfigurationId", credentialConfiguration.credentialConfigurationId())
                        .param("subjectIdentifier", "abc-123-anything"))
                .andExpect(status().isOk())
                .andExpect(view().name("revoke"))
                .andExpect(model().attribute("subjectSuccessMessage", "Beviset er revokert dersom det eksisterte."));

        verify(issuerServerService).revokeCredentialBySubject(eq(credentialConfiguration), eq("abc-123-anything"));
    }

    @Test
    void postRevokeBySubjectReturnsErrorWhenCredentialConfigurationIsUnknown() throws Exception {
        when(issuerServerService.getById("unknown")).thenReturn(null);

        mockMvc.perform(post("/revoke/by-subject")
                        .param("credentialConfigurationId", "unknown")
                        .param("subjectIdentifier", "some-subject"))
                .andExpect(status().isOk())
                .andExpect(view().name("revoke"))
                .andExpect(model().attribute("subjectErrorMessage", "Credential configuration finnes ikkje"));
    }

    @Test
    void postRevokeBySubjectReturnsErrorWhenIssuerServerFails() throws Exception {
        when(issuerServerService.getById(credentialConfiguration.credentialConfigurationId())).thenReturn(credentialConfiguration);

        HttpClientErrorException cause = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                null,
                new byte[0],
                null
        );
        doThrow(new IssuerServerException("revoke by subject failed", cause))
                .when(issuerServerService)
                .revokeCredentialBySubject(eq(credentialConfiguration), anyString());

        mockMvc.perform(post("/revoke/by-subject")
                        .param("credentialConfigurationId", credentialConfiguration.credentialConfigurationId())
                        .param("subjectIdentifier", "05821098825"))
                .andExpect(status().isOk())
                .andExpect(view().name("revoke"))
                .andExpect(model().attributeExists("subjectErrorMessage"));
    }

}
