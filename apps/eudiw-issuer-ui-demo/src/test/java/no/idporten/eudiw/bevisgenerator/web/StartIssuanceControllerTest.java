package no.idporten.eudiw.bevisgenerator.web;

import jakarta.servlet.ServletException;
import no.idporten.eudiw.bevisgenerator.config.BevisgeneratorProperties;
import no.idporten.eudiw.bevisgenerator.config.FeatureSwitches;
import no.idporten.eudiw.bevisgenerator.exception.IssuerUiException;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.IssuerServerService;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.CredentialConfiguration;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.IssuerServerProperties;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.domain.CredentialOffer;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.domain.Grants;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.domain.IssuanceResponse;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.domain.IssuanceStatus;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.domain.IssuanceStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceView;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class StartIssuanceControllerTest {

    private static final String TRANSACTION_CONFIGURATION_KEY = "issuance_credential_configuration_tx-id";
    private static final String TRANSACTION_COMPLETED_KEY = "issuance_completed_tx-id";
    private static final String TRANSACTION_DESCRIPTION_KEY = "issuance_credential_description_tx-id";
    private static final String TRANSACTION_SUBJECT_IDENTIFIER_KEY = "issuance_subject_identifier_tx-id";

    private MockMvc mockMvc;
    private IssuerServerService issuerServerService;
    private CredentialConfiguration credentialConfiguration;

    @BeforeEach
    void setUp() {
        issuerServerService = mock(IssuerServerService.class);
        IssuerServerProperties properties = mock(IssuerServerProperties.class);
        BevisgeneratorProperties bevisgeneratorProperties = mock(BevisgeneratorProperties.class);
        FeatureSwitches featureSwitches = mock(FeatureSwitches.class);

        credentialConfiguration = new CredentialConfiguration(
                "http://issuer/tenant",
                "pid",
                "eudiw:pid",
                null,
                "PID",
                "{}"
        );

        when(properties.credentialIssuer()).thenReturn("http://issuer");
        when(bevisgeneratorProperties.getFeatureSwitches()).thenReturn(featureSwitches);
        when(issuerServerService.getById("pid")).thenReturn(credentialConfiguration);
        when(issuerServerService.getSubjectCredentialConfigurationById("pid")).thenReturn(credentialConfiguration);
        when(issuerServerService.getAll()).thenReturn(List.of(credentialConfiguration));

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new StartIssuanceController(
                                issuerServerService,
                                properties,
                                bevisgeneratorProperties
                        ))
                .setViewResolvers((viewName, locale) -> {
                    InternalResourceView view = new InternalResourceView();
                    view.setUrl("/templates/" + viewName + ".html");
                    return view;
                })
                .build();
    }

    @Test
    void startIssuanceStoresTransactionInSessionAndShowsWaitingView() throws Exception {
        IssuanceResponse response = new IssuanceResponse(
                new CredentialOffer(
                        "http://issuer/tenant",
                        List.of("pid"),
                        new Grants(null, null)
                ),
                "tx-id"
        );
        when(issuerServerService.startIssuance(eq(credentialConfiguration), any())).thenReturn(response);

        mockMvc.perform(post("/start-issuance/pid")
                        .param("json", "{}")
                        .param("personIdentifier", "05821098825"))
                .andExpect(status().isOk())
                .andExpect(view().name("issuer_response"))
                .andExpect(model().attribute("issuedTransactionId", "tx-id"))
                .andExpect(request().sessionAttribute(TRANSACTION_CONFIGURATION_KEY, "pid"))
                .andExpect(request().sessionAttribute(TRANSACTION_DESCRIPTION_KEY, "PID"))
                .andExpect(request().sessionAttribute(TRANSACTION_SUBJECT_IDENTIFIER_KEY, "05821098825"));
    }

    @Test
    void issuanceStatusReturnsAcceptedWhileWaitingForIssuer() throws Exception {
        when(issuerServerService.retrieveIssuanceStatus(credentialConfiguration, "tx-id"))
                .thenReturn(new IssuanceStatusResponse("tx-id", IssuanceStatus.OFFER_ISSUED));

        mockMvc.perform(get("/issuance/tx-id/status")
                        .sessionAttr(TRANSACTION_CONFIGURATION_KEY, "pid"))
                .andExpect(status().isAccepted());
    }

    @ParameterizedTest
    @ValueSource(strings = {"credential_issued", "credential_accepted"})
    void issuanceStatusMarksIssuedCredentialAsCompleted(String issuerStatus) throws Exception {
        when(issuerServerService.retrieveIssuanceStatus(credentialConfiguration, "tx-id"))
                .thenReturn(new IssuanceStatusResponse("tx-id", IssuanceStatus.fromValue(issuerStatus)));

        mockMvc.perform(get("/issuance/tx-id/status")
                        .sessionAttr(TRANSACTION_CONFIGURATION_KEY, "pid"))
                .andExpect(status().isOk())
                .andExpect(request().sessionAttribute(TRANSACTION_COMPLETED_KEY, true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"credential_failure", "credential_deleted"})
    void issuanceStatusReturnsUnprocessableContentForWalletFailure(String issuerStatus) throws Exception {
        when(issuerServerService.retrieveIssuanceStatus(credentialConfiguration, "tx-id"))
                .thenReturn(new IssuanceStatusResponse("tx-id", IssuanceStatus.fromValue(issuerStatus)));

        mockMvc.perform(get("/issuance/tx-id/status")
                        .sessionAttr(TRANSACTION_CONFIGURATION_KEY, "pid"))
                .andExpect(status().isUnprocessableContent());
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", ""})
    void issuanceStatusReturnsNotFoundForUnknownStatus(String issuerStatus) throws Exception {
        when(issuerServerService.retrieveIssuanceStatus(credentialConfiguration, "tx-id"))
                .thenReturn(new IssuanceStatusResponse("tx-id", IssuanceStatus.fromValue(issuerStatus)));

        mockMvc.perform(get("/issuance/tx-id/status")
                        .sessionAttr(TRANSACTION_CONFIGURATION_KEY, "pid"))
                .andExpect(status().isNotFound());
    }

    @Test
    void issuanceStatusReturnsServerErrorForUnexpectedStatus() throws Exception {
        when(issuerServerService.retrieveIssuanceStatus(credentialConfiguration, "tx-id"))
                .thenReturn(new IssuanceStatusResponse("tx-id", IssuanceStatus.UNRECOGNIZED));

        mockMvc.perform(get("/issuance/tx-id/status")
                        .sessionAttr(TRANSACTION_CONFIGURATION_KEY, "pid"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void issuanceStatusRequiresTransactionInSession() {
        ServletException exception = assertThrows(
                ServletException.class,
                () -> mockMvc.perform(get("/issuance/tx-id/status"))
        );

        assertInstanceOf(IssuerUiException.class, exception.getCause());
    }

    @Test
    void completedIssuanceReturnsCompletionViewWithoutCaching() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(TRANSACTION_CONFIGURATION_KEY, "pid");
        session.setAttribute(TRANSACTION_COMPLETED_KEY, true);
        session.setAttribute(TRANSACTION_DESCRIPTION_KEY, "PID");
        session.setAttribute(TRANSACTION_SUBJECT_IDENTIFIER_KEY, "05821098825");
        clearInvocations(issuerServerService);

        mockMvc.perform(get("/issuance/tx-id/complete").session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store, private"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(view().name("issuance-complete"))
                .andExpect(model().attribute("issuedCredentialConfigurationId", "pid"))
                .andExpect(model().attribute("issuedCredentialDescription", "PID"))
                .andExpect(model().attribute("issuedTransactionId", "tx-id"))
                .andExpect(model().attribute("issuedSubjectIdentifier", "05821098825"));

        verify(issuerServerService, never()).getById(anyString());
    }

    @Test
    void completedIssuanceFallsBackToConfigurationIdWhenDescriptionIsMissing() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(TRANSACTION_CONFIGURATION_KEY, "pid");
        session.setAttribute(TRANSACTION_COMPLETED_KEY, true);

        mockMvc.perform(get("/issuance/tx-id/complete").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("issuedCredentialDescription", "pid"))
                .andExpect(model().attribute("issuedSubjectIdentifier", ""));
    }

    @Test
    void completionViewRequiresAcceptedCredential() {
        ServletException exception = assertThrows(
                ServletException.class,
                () -> mockMvc.perform(get("/issuance/tx-id/complete")
                        .sessionAttr(TRANSACTION_CONFIGURATION_KEY, "pid"))
        );

        assertInstanceOf(IssuerUiException.class, exception.getCause());
    }
}
