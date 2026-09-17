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
import no.idporten.eudiw.bevisgenerator.web.models.IssuanceSessionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.view.InternalResourceView;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class StartIssuanceControllerTest {

    private static final String TRANSACTION_KEY = "issuance_session_tx-id";
    private MockMvc mockMvc;
    private IssuerServerService issuerServerService;
    private IssuerServerProperties issuerServerProperties;
    private BevisgeneratorProperties bevisgeneratorProperties;
    private CredentialConfiguration credentialConfiguration;

    @BeforeEach
    void setUp() {
        issuerServerService = mock(IssuerServerService.class);
        issuerServerProperties = mock(IssuerServerProperties.class);
        bevisgeneratorProperties = mock(BevisgeneratorProperties.class);
        FeatureSwitches featureSwitches = mock(FeatureSwitches.class);

        credentialConfiguration = new CredentialConfiguration(
                "http://issuer/tenant",
                "pid",
                "eudiw:pid",
                null,
                "PID",
                "{}"
        );

        when(issuerServerProperties.credentialIssuer()).thenReturn("http://issuer");
        when(bevisgeneratorProperties.getFeatureSwitches()).thenReturn(featureSwitches);
        when(issuerServerService.getById("pid")).thenReturn(credentialConfiguration);
        when(issuerServerService.getSubjectCredentialConfigurationById("pid")).thenReturn(credentialConfiguration);
        when(issuerServerService.getAll()).thenReturn(List.of(credentialConfiguration));

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new StartIssuanceController(
                                issuerServerService,
                                issuerServerProperties,
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
    void issueSortsCredentialConfigurationsUsingNorwegianAlphabeticalOrder() throws Exception {
        CredentialConfiguration a = credentialConfiguration("a", "Aldersbevis");
        CredentialConfiguration ae = credentialConfiguration("ae", "Æresbevis");
        CredentialConfiguration o = credentialConfiguration("o", "Øvingsbevis");
        CredentialConfiguration aa = credentialConfiguration("aa", "Årsbevis");
        when(issuerServerService.getAll()).thenReturn(List.of(aa, o, ae, a));

        var result = mockMvc.perform(get("/issue"))
                .andExpect(status().isOk())
                .andReturn();

        List<?> credentialConfigurations = (List<?>) result.getModelAndView()
                .getModel()
                .get("credential_configurations");
        assertEquals(
                List.of("Aldersbevis", "Æresbevis", "Øvingsbevis", "Årsbevis"),
                credentialConfigurations.stream()
                        .map(configuration -> ((StartIssuanceController.IssueCredentialConfiguration) configuration).credentialName())
                        .toList()
        );
    }

    @Test
    void issueUsesConfigurationIdWhenDescriptionIsMissingOrBlank() throws Exception {
        CredentialConfiguration missingDescription = credentialConfiguration("missing-description", null);
        CredentialConfiguration blankDescription = credentialConfiguration("blank-description", " ");
        when(issuerServerService.getAll()).thenReturn(List.of(missingDescription, blankDescription));

        var result = thymeleafMockMvc().perform(get("/issue"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("blank-description")))
                .andExpect(content().string(containsString("missing-description")))
                .andReturn();

        List<?> credentialConfigurations = (List<?>) result.getModelAndView()
                .getModel()
                .get("credential_configurations");
        assertEquals(
                List.of("blank-description", "missing-description"),
                credentialConfigurations.stream()
                        .map(configuration -> ((StartIssuanceController.IssueCredentialConfiguration) configuration).credentialName())
                        .toList()
        );
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
                .andExpect(model().attribute("credentialName", "PID"))
                .andExpect(request().sessionAttribute(TRANSACTION_KEY, issuanceSession(false)));
    }

    @Test
    void startIssuanceRejectsUnexpectedCredentialOffer() {
        IssuanceResponse response = new IssuanceResponse(
                new CredentialOffer(
                        "http://issuer/tenant",
                        List.of("another-credential"),
                        new Grants(null, null)
                ),
                "tx-id"
        );
        when(issuerServerService.startIssuance(eq(credentialConfiguration), any())).thenReturn(response);

        ServletException exception = assertThrows(
                ServletException.class,
                () -> mockMvc.perform(post("/start-issuance/pid").param("json", "{}"))
        );

        assertInstanceOf(IssuerUiException.class, exception.getCause());
    }

    @Test
    void issuerResponseRendersCredentialNameAboveQrCode() throws Exception {
        IssuanceResponse response = new IssuanceResponse(
                new CredentialOffer(
                        "http://issuer/tenant",
                        List.of("pid"),
                        new Grants(null, null)
                ),
                "tx-id"
        );
        when(issuerServerService.startIssuance(eq(credentialConfiguration), any())).thenReturn(response);

        thymeleafMockMvc().perform(post("/start-issuance/pid")
                        .param("json", "{}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">PID</h2>")))
                .andExpect(content().string(containsString("alt=\"QR-kode for PID\"")));
    }

    @Test
    void issuanceStatusReturnsAcceptedWhileWaitingForIssuer() throws Exception {
        when(issuerServerService.retrieveIssuanceStatus(credentialConfiguration, "tx-id"))
                .thenReturn(new IssuanceStatusResponse("tx-id", IssuanceStatus.OFFER_ISSUED));

        mockMvc.perform(get("/issuance/tx-id/status")
                        .sessionAttr(TRANSACTION_KEY, issuanceSession(false)))
                .andExpect(status().isAccepted());
    }

    @ParameterizedTest
    @ValueSource(strings = {"credential_issued", "credential_accepted"})
    void issuanceStatusMarksIssuedCredentialAsCompleted(String issuerStatus) throws Exception {
        when(issuerServerService.retrieveIssuanceStatus(credentialConfiguration, "tx-id"))
                .thenReturn(new IssuanceStatusResponse("tx-id", IssuanceStatus.fromValue(issuerStatus)));

        mockMvc.perform(get("/issuance/tx-id/status")
                        .sessionAttr(TRANSACTION_KEY, issuanceSession(false)))
                .andExpect(status().isOk())
                .andExpect(request().sessionAttribute(TRANSACTION_KEY, issuanceSession(true)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"credential_failure", "credential_deleted"})
    void issuanceStatusReturnsUnprocessableContentForWalletFailure(String issuerStatus) throws Exception {
        when(issuerServerService.retrieveIssuanceStatus(credentialConfiguration, "tx-id"))
                .thenReturn(new IssuanceStatusResponse("tx-id", IssuanceStatus.fromValue(issuerStatus)));

        mockMvc.perform(get("/issuance/tx-id/status")
                        .sessionAttr(TRANSACTION_KEY, issuanceSession(false)))
                .andExpect(status().isUnprocessableContent());
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", ""})
    void issuanceStatusReturnsNotFoundForUnknownStatus(String issuerStatus) throws Exception {
        when(issuerServerService.retrieveIssuanceStatus(credentialConfiguration, "tx-id"))
                .thenReturn(new IssuanceStatusResponse("tx-id", IssuanceStatus.fromValue(issuerStatus)));

        mockMvc.perform(get("/issuance/tx-id/status")
                        .sessionAttr(TRANSACTION_KEY, issuanceSession(false)))
                .andExpect(status().isNotFound());
    }

    @Test
    void issuanceStatusReturnsServerErrorForUnexpectedStatus() throws Exception {
        when(issuerServerService.retrieveIssuanceStatus(credentialConfiguration, "tx-id"))
                .thenReturn(new IssuanceStatusResponse("tx-id", IssuanceStatus.UNRECOGNIZED));

        mockMvc.perform(get("/issuance/tx-id/status")
                        .sessionAttr(TRANSACTION_KEY, issuanceSession(false)))
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
        session.setAttribute(TRANSACTION_KEY, issuanceSession(true));
        clearInvocations(issuerServerService);

        mockMvc.perform(get("/issuance/tx-id/complete").session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store, private"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(view().name("issuance-complete"))
                .andExpect(model().attribute("issuedCredentialConfigurationId", "pid"))
                .andExpect(model().attribute("issuedCredentialDescription", "PID"))
                .andExpect(model().attribute("issuedTransactionId", "tx-id"))
                .andExpect(model().attribute("issuedSubjectIdentifier", "05821098825"))
                .andExpect(model().attribute("issuedCredentialIssuer", "http://issuer/tenant"))
                .andExpect(model().attribute("claims", List.of()))
                .andExpect(model().attribute("steps", List.of("Vel bevistype", "Skann QR-kode", "Utferding fullført")));

        verify(issuerServerService, never()).getById(anyString());
    }

    @Test
    void issueRendersStepperWithFirstStepCurrent() throws Exception {
        thymeleafMockMvc().perform(get("/issue"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"stepper\"")))
                .andExpect(content().string(containsString("aria-label=\"Steg\"")))
                .andExpect(content().string(containsString("aria-current=\"step\"")))
                .andExpect(content().string(containsString("Vel bevistype")))
                .andExpect(content().string(containsString("Skann QR-kode")))
                .andExpect(content().string(containsString("Utferding fullført")));
    }

    @Test
    void startRendersStepperWithFirstStepCurrent() throws Exception {
        thymeleafMockMvc().perform(get("/start-issuance/pid"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"stepper\"")))
                .andExpect(content().string(containsString("aria-current=\"step\"")))
                .andExpect(content().string(containsString("Vel bevistype")));
    }

    @Test
    void issuerResponseRendersStepperWithSecondStepCurrent() throws Exception {
        IssuanceResponse response = new IssuanceResponse(
                new CredentialOffer(
                        "http://issuer/tenant",
                        List.of("pid"),
                        new Grants(null, null)
                ),
                "tx-id"
        );
        when(issuerServerService.startIssuance(eq(credentialConfiguration), any())).thenReturn(response);

        thymeleafMockMvc().perform(post("/start-issuance/pid").param("json", "{}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"stepper\"")))
                .andExpect(content().string(containsString("aria-current=\"step\"")))
                .andExpect(content().string(containsString("Skann QR-kode")));
    }

    @Test
    void startIssuanceExtractsCredentialDataClaims() throws Exception {
        IssuanceResponse response = new IssuanceResponse(
                new CredentialOffer(
                        "http://issuer/tenant",
                        List.of("pid"),
                        new Grants(null, null)
                ),
                "tx-id"
        );
        when(issuerServerService.startIssuance(eq(credentialConfiguration), any())).thenReturn(response);

        String jsonWithClaims = """
                {
                  "subject": { "identifier": "05821098825" },
                  "credential_data": {
                    "given_name": "Ola",
                    "birth_date": "1990-01-01"
                  }
                }
                """;

        mockMvc.perform(post("/start-issuance/pid")
                        .param("json", jsonWithClaims)
                        .param("personIdentifier", "05821098825"))
                .andExpect(status().isOk())
                .andExpect(request().sessionAttribute(
                        TRANSACTION_KEY,
                        new IssuanceSessionData(
                                "http://issuer/tenant",
                                "pid",
                                "PID",
                                "05821098825",
                                false,
                                List.of(
                                        new no.idporten.eudiw.bevisgenerator.web.models.ClaimView(
                                                "given_name", "given name", "Ola", List.of()
                                        ),
                                        new no.idporten.eudiw.bevisgenerator.web.models.ClaimView(
                                                "birth_date", "birth date", "1990-01-01", List.of()
                                        )
                                )
                        )
                ));
    }

    @Test
    void completedIssuanceRendersStepperAndTwoCardsWithClaims() throws Exception {
        MockHttpSession session = new MockHttpSession();
        IssuanceSessionData sessionData = new IssuanceSessionData(
                "http://issuer/tenant",
                "pid",
                "MinID PID",
                "05821098825",
                true,
                List.of(
                        new no.idporten.eudiw.bevisgenerator.web.models.ClaimView(
                                "given_name", "Førenamn", "Kari", List.of()
                        )
                )
        );
        session.setAttribute(TRANSACTION_KEY, sessionData);

        thymeleafMockMvc().perform(get("/issuance/tx-id/complete").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"stepper\"")))
                .andExpect(content().string(containsString("aria-current=\"step\"")))
                .andExpect(content().string(containsString("Utferding fullført")))
                .andExpect(content().string(containsString("Attributtar</h2>")))
                .andExpect(content().string(containsString("Utferdingsdetaljar</h2>")))
                .andExpect(content().string(containsString("Beviset er utferda til lommeboka")))
                .andExpect(content().string(containsString("Førenamn")))
                .andExpect(content().string(containsString("Kari")))
                .andExpect(content().string(containsString("Bevistype")))
                .andExpect(content().string(containsString("pid")))
                .andExpect(content().string(containsString("Personidentifikator")))
                .andExpect(content().string(containsString("05821098825")))
                .andExpect(content().string(containsString("Utferdar")))
                .andExpect(content().string(containsString("http://issuer/tenant")))
                .andExpect(content().string(containsString("Transaksjons-ID")))
                .andExpect(content().string(containsString("tx-id")));
    }

    @Test
    void completedIssuanceRendersGroupClaimsInSeparateBox() throws Exception {
        MockHttpSession session = new MockHttpSession();
        IssuanceSessionData sessionData = new IssuanceSessionData(
                "http://issuer/tenant",
                "pid",
                "MinID PID",
                "05821098825",
                true,
                List.of(
                        new no.idporten.eudiw.bevisgenerator.web.models.ClaimView(
                                "driving_privileges", "Driving privileges", null,
                                List.of(
                                        new no.idporten.eudiw.bevisgenerator.web.models.ClaimView(
                                                "issue_date", "Issue date", "2026-03-12", List.of()
                                        )
                                )
                        )
                )
        );
        session.setAttribute(TRANSACTION_KEY, sessionData);

        thymeleafMockMvc().perform(get("/issuance/tx-id/complete").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"claim-group\"")))
                .andExpect(content().string(containsString("claim-group-heading\"")))
                .andExpect(content().string(containsString("Driving privileges</h3>")))
                .andExpect(content().string(containsString("Issue date")))
                .andExpect(content().string(containsString("2026-03-12")));
    }

    @Test
    void completedIssuanceRendersFallbackWhenClaimsAreEmpty() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(TRANSACTION_KEY, issuanceSession(true));

        thymeleafMockMvc().perform(get("/issuance/tx-id/complete").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dette beviset hentar data automatisk frå registeret")));
    }

    @Test
    void completedIssuancePostsPresentationForIssuedCredential() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(TRANSACTION_KEY, issuanceSession(true));

        thymeleafMockMvc().perform(get("/issuance/tx-id/complete").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"automatic-verification-form\"")))
                .andExpect(content().string(containsString("action=\"/verification-start\"")))
                .andExpect(content().string(containsString("method=\"post\"")))
                .andExpect(content().string(containsString("name=\"issuanceTransactionId\"")))
                .andExpect(content().string(containsString("value=\"tx-id\"")))
                .andExpect(content().string(containsString("form=\"automatic-verification-form\"")));
    }

    @Test
    void startIssuanceFallsBackToConfigurationIdWhenDescriptionIsMissing() throws Exception {
        CredentialConfiguration unnamedCredential = credentialConfiguration("pid", " ");
        IssuanceResponse response = new IssuanceResponse(
                new CredentialOffer(
                        "http://issuer/tenant",
                        List.of("pid"),
                        new Grants(null, null)
                ),
                "tx-id"
        );
        when(issuerServerService.getById("pid")).thenReturn(unnamedCredential);
        when(issuerServerService.startIssuance(eq(unnamedCredential), any())).thenReturn(response);

        mockMvc.perform(post("/start-issuance/pid").param("json", "{}"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("credentialName", "pid"))
                .andExpect(request().sessionAttribute(
                        TRANSACTION_KEY,
                        new IssuanceSessionData(
                                "http://issuer/tenant",
                                "pid",
                                "pid",
                                "",
                                false
                        )
                ));
    }

    @Test
    void completionViewRequiresAcceptedCredential() {
        ServletException exception = assertThrows(
                ServletException.class,
                () -> mockMvc.perform(get("/issuance/tx-id/complete")
                        .sessionAttr(TRANSACTION_KEY, issuanceSession(false)))
        );

        assertInstanceOf(IssuerUiException.class, exception.getCause());
    }

    private CredentialConfiguration credentialConfiguration(String id, String description) {
        return new CredentialConfiguration(
                "http://issuer/tenant",
                id,
                "eudiw:" + id,
                null,
                description,
                "{}"
        );
    }

    private static IssuanceSessionData issuanceSession(boolean completed) {
        return new IssuanceSessionData(
                "http://issuer/tenant",
                "pid",
                "PID",
                "05821098825",
                completed
        );
    }

    private MockMvc thymeleafMockMvc() {
        GenericWebApplicationContext applicationContext = new GenericWebApplicationContext();
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

        return MockMvcBuilders.standaloneSetup(
                        new StartIssuanceController(
                                issuerServerService,
                                issuerServerProperties,
                                bevisgeneratorProperties
                        ))
                .setViewResolvers(viewResolver)
                .build();
    }
}
