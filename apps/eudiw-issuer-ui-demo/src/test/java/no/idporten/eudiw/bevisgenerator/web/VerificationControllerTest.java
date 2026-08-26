package no.idporten.eudiw.bevisgenerator.web;

import no.idporten.eudiw.bevisgenerator.integration.issuerserver.IssuerServerService;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.IssuerServerProperties;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.credentialdefinitionmodel.ClaimMetadata;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.credentialdefinitionmodel.CredentialConfiguration;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.credentialdefinitionmodel.CredentialConfigurationMetadata;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.credentialdefinitionmodel.CredentialIssuerMetadata;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.*;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.*;
import no.idporten.eudiw.bevisgenerator.integration.byobservice.model.Display;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.servlet.view.RedirectView;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VerificationControllerTest {

    private MockMvc mockMvc;
    private VerifierService verifierService;
    private String issuanceDefinitionId;
    private VerificationTransactionData verificationTransactionData;

    @BeforeEach
    void setUp() {
        IssuerServerService issuerServerService = mock(IssuerServerService.class);
        IssuerServerProperties issuerServerProperties = mock(IssuerServerProperties.class);
        verifierService = mock(VerifierService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        DCQLService dcqlService = new DCQLServiceImpl();
        VerificationResultService verificationResultService = new VerificationResultServiceImpl();

        issuanceDefinitionId = "pid";
        String subjectDefinitionId = "proof_of_age";
        CredentialConfiguration issuanceConfig = new CredentialConfiguration(
                null,
                "no:kontaktregisteret:kontaktinformasjon:1",
                "scope",
                "dc+sd-jwt",
                List.of(),
                List.of(),
                new CredentialConfigurationMetadata(
                        List.of(new Display("PID")),
                        List.of(
                                new ClaimMetadata(List.of("personidentifikator"), true, List.of(new Display("Personidentifikator"))),
                                new ClaimMetadata(List.of("epostadresse"), false, List.of(new Display("E-postadresse")))
                        )
                ),
                Map.of()
        );
        CredentialConfiguration subjectConfig = new CredentialConfiguration(
                "eu.europa.ec.eudiw.age_over_18",
                null,
                "proof_of_age",
                "mso_mdoc",
                List.of(),
                List.of(),
                new CredentialConfigurationMetadata(
                        List.of(new Display("Aldersbevis")),
                        List.of(new ClaimMetadata(List.of("age_over_18"), true, List.of(new Display("Over 18"))))
                ),
                Map.of()
        );
        CredentialIssuerMetadata credentialIssuerMetadata = new CredentialIssuerMetadata(
                "http://issuer",
                List.of(),
                "http://issuer/credential",
                null,
                null,
                Map.of(issuanceDefinitionId, issuanceConfig, subjectDefinitionId, subjectConfig),
                List.of()
        );

        verificationTransactionData = new VerificationTransactionData(
                new VerificationStartResponse("eudi-openid4vp://example", "data:image/png;base64,abc123", "tx-id"),
                URI.create("http://verifier/start"),
                "{\"dcql_query\":{\"credentials\":[]}}",
                URI.create("http://verifier/status/tx-id"),
                URI.create("http://verifier/result/tx-id")
        );

        when(issuerServerProperties.credentialIssuer()).thenReturn("http://issuer");
        when(issuerServerService.getAllCredentialIssuerMetadata()).thenReturn(List.of(credentialIssuerMetadata));
        when(verifierService.startVerification(anyString())).thenReturn(
                new VerificationTransactionData(
                        new VerificationStartResponse("eudi-openid4vp://example", "data:image/png;base64,abc123", "tx-id"),
                        URI.create("http://verifier/start"),
                        """
                        {
                          "dcql_query": {"credentials":[]}
                        }""",
                        URI.create("http://verifier/status/tx-id"),
                        URI.create("http://verifier/result/tx-id")
                ));
        when(verifierService.retrieveVerificationResult("tx-id")).thenReturn(new VerificationResult(
                "tx-id",
                Map.of(
                        "proof_of_age",
                        List.of(new VerifiedCredential( Map.of("age_over_18", true), true, List.of())
                )
        )));

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new VerificationController(issuerServerService, issuerServerProperties, verifierService, objectMapper, dcqlService, verificationResultService))
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
                .andExpect(model().attributeExists("credentialDefinitions"))
                .andExpect(model().attributeExists("credentialDefinitionsJson"))
                .andExpect(model().attributeExists("selectedClaimPathsJson"));
    }

    @Test
    void getVerificationStartRendersTemplateWithCredentialCardGrid() throws Exception {
        // Uses a real Thymeleaf view resolver (instead of the standalone InternalResourceView
        // stub) to verify the actual template renders without errors after the Alpine.js
        // card-grid refactor (EUW-1742), including partials such as fragments/layout.html.
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

        IssuerServerService issuerServerService = mock(IssuerServerService.class);
        IssuerServerProperties issuerServerProperties = mock(IssuerServerProperties.class);
        when(issuerServerProperties.credentialIssuer()).thenReturn("http://issuer");
        when(issuerServerService.getAllCredentialIssuerMetadata()).thenReturn(List.of(
                new CredentialIssuerMetadata(
                        "http://issuer",
                        List.of(),
                        "http://issuer/credential",
                        null,
                        null,
                        Map.of("pid", new CredentialConfiguration(
                                null,
                                "no:kontaktregisteret:kontaktinformasjon:1",
                                "scope",
                                "dc+sd-jwt",
                                List.of(),
                                List.of(),
                                new CredentialConfigurationMetadata(
                                        List.of(new Display("PID")),
                                        List.of(new ClaimMetadata(List.of("personidentifikator"), true, List.of(new Display("Personidentifikator"))))
                                ),
                                Map.of()
                        )),
                        List.of()
                )
        ));

        MockMvc thymeleafMockMvc = MockMvcBuilders.standaloneSetup(
                        new VerificationController(issuerServerService, issuerServerProperties, verifierService, new ObjectMapper(), new DCQLServiceImpl(), new VerificationResultServiceImpl()))
                .setValidator(validator)
                .setViewResolvers(viewResolver)
                .build();

        thymeleafMockMvc.perform(get("/verification-start"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"step-flow\"")))
                .andExpect(content().string(containsString("credential-picker")))
                .andExpect(content().string(containsString("credential-grid")))
                .andExpect(content().string(containsString("credentialPicker(")))
                .andExpect(content().string(containsString("Start verifisering")))
                .andExpect(content().string(containsString("x-bind:disabled=\"!selectedId\"")));

        thymeleafMockMvc.perform(get("/verification-result/uniqueKey")
                        .sessionAttr("verification_transaction_data_uniqueKey", verificationTransactionData))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"step-flow\"")))
                .andExpect(content().string(containsString("class=\"step-flow__content\"")))
                .andExpect(content().string(containsString("href=\"/verification-start\"")))
                .andExpect(content().string(containsString("Presenter nytt bevis")))
                .andExpect(content().string(containsString("href=\"/revoke\"")))
                .andExpect(content().string(containsString("Tilbakekall bevis")))
                .andExpect(content().string(containsString("age over 18")))
                .andExpect(content().string(containsString("Ja")))
                .andExpect(content().string(containsString("Beviset er gyldig")))
                .andExpect(content().string(containsString("Attributt")))
                .andExpect(content().string(containsString("Valideringsdetaljar")))
                .andExpect(content().string(containsString("verification-result__claims")))
                .andExpect(content().string(not(containsString(">Heim<"))));

        VerificationTransactionData nestedTransactionData = new VerificationTransactionData(
                new VerificationStartResponse("eudi-openid4vp://example", "data:image/png;base64,abc123", "tx-id-nested"),
                URI.create("http://verifier/start"),
                "{\"dcql_query\":{\"credentials\":[]}}",
                URI.create("http://verifier/status/tx-id-nested"),
                URI.create("http://verifier/result/tx-id-nested")
        );
        when(verifierService.retrieveVerificationResult("tx-id-nested")).thenReturn(new VerificationResult(
                "tx-id-nested",
                Map.of(
                        "pid",
                        List.of(new VerifiedCredential(
                                Map.of(
                                        "fornavn", "Kari",
                                        "adresse", Map.of("gate", "Fjordveien 1", "postnummer", "0150"),
                                        "statsborgerskap", List.of("NO", "SE")
                                ),
                                true,
                                List.of()
                        ))
                )
        ));

        // Nested (object/array) claim values must render as their own indented name/value
        // rows via the recursive claim_list_fragment, not as a raw JSON dump in a single <dd>.
        thymeleafMockMvc.perform(get("/verification-result/nestedKey")
                        .sessionAttr("verification_transaction_data_nestedKey", nestedTransactionData))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("adresse")))
                .andExpect(content().string(containsString("gate")))
                .andExpect(content().string(containsString("Fjordveien 1")))
                .andExpect(content().string(containsString("postnummer")))
                .andExpect(content().string(containsString("0150")))
                .andExpect(content().string(containsString("statsborgerskap")))
                .andExpect(content().string(containsString("NO, SE")))
                .andExpect(content().string(containsString("verification-result__claim--group")))
                .andExpect(content().string(not(containsString("{\"gate\""))));

        // mso_mdoc credentials wrap every claim under a single namespace key
        // (e.g. "eu.europa.ec.eudi.pid.1"). That wrapper must be unwrapped so claims
        // render flat, instead of showing the raw namespace string as a confusing group.
        VerificationTransactionData mdocTransactionData = new VerificationTransactionData(
                new VerificationStartResponse("eudi-openid4vp://example", "data:image/png;base64,abc123", "tx-id-mdoc"),
                URI.create("http://verifier/start"),
                "{\"dcql_query\":{\"credentials\":[]}}",
                URI.create("http://verifier/status/tx-id-mdoc"),
                URI.create("http://verifier/result/tx-id-mdoc")
        );
        when(verifierService.retrieveVerificationResult("tx-id-mdoc")).thenReturn(new VerificationResult(
                "tx-id-mdoc",
                Map.of(
                        "no.digdir.eudiw.pid_mso_mdoc",
                        List.of(new VerifiedCredential(
                                Map.of(
                                        "eu.europa.ec.eudi.pid.1",
                                        Map.of(
                                                "personal_administrative_number", "12345678912",
                                                "given_name", "Kari"
                                        )
                                ),
                                true,
                                List.of()
                        ))
                )
        ));

        thymeleafMockMvc.perform(get("/verification-result/mdocKey")
                        .sessionAttr("verification_transaction_data_mdocKey", mdocTransactionData))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("personal administrative number")))
                .andExpect(content().string(containsString("12345678912")))
                .andExpect(content().string(containsString("given name")))
                .andExpect(content().string(containsString("Kari")))
                .andExpect(content().string(not(containsString("eu.europa.ec.eudi.pid.1"))));
    }

    @Test
    void getVerificationStartContainsAllCredentialDefinitions() throws Exception {
        mockMvc.perform(get("/verification-start"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("credentialDefinitions", hasSize(2)));
    }

    @Test
    void postVerificationStartWithValidInputRedirectsToPresentation() throws Exception {
        mockMvc.perform(post("/verification-start")
                        .param("credentialConfigurationId", issuanceDefinitionId)
                        .param("selectedClaimPaths", "epostadresse"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/verification-presentation/*"));

        verify(verifierService).startVerification(argThat(requestBody ->
                requestBody.contains("\"dcql_query\"")
                        && requestBody.contains("\"id\"")
                        && requestBody.contains("\"format\":\"dc+sd-jwt\"")
                        && requestBody.contains("\"redirect_uri\":\"http://localhost/verification-presentation/")
                        && !requestBody.contains("\"personidentifikator\"")
                        && requestBody.contains("\"epostadresse\"")
        ));
    }

    @Test
    void getVerificationPresentationReturnsPresentationView() throws Exception {
        mockMvc.perform(get("/verification-presentation/uniqueKey")
                        .sessionAttr("verification_transaction_data_uniqueKey", verificationTransactionData))
                .andExpect(status().isOk())
                .andExpect(view().name("verification-presentation"))
                .andExpect(model().attributeExists("qrCode"))
                .andExpect(model().attributeExists("authorizationRequest"))
                .andExpect(model().attributeExists("transactionId"))
                .andExpect(model().attributeExists("statusUri"))
                .andExpect(model().attributeExists("requestBody"))
                .andExpect(model().attributeExists("requestUri"))
                .andExpect(model().attributeExists("responseBody"));
    }

    @Test
    void getVerificationResultAddsResultAndPrettyJsonToModel() throws Exception {
        mockMvc.perform(get("/verification-result/uniqueKey")
                        .sessionAttr("verification_transaction_data_uniqueKey", verificationTransactionData))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store, private"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(view().name("verification-result"))
                .andExpect(model().attributeExists("result"))
                .andExpect(model().attribute("verificationResults", hasSize(1)));

        verify(verifierService).retrieveVerificationResult("tx-id");
    }

    @Test
    void getVerificationResultReusesResultFromSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("verification_transaction_data_uniqueKey", verificationTransactionData);

        mockMvc.perform(get("/verification-result/uniqueKey").session(session))
                .andExpect(status().isOk());
        mockMvc.perform(get("/verification-result/uniqueKey").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("result"));

        verify(verifierService, times(1)).retrieveVerificationResult("tx-id");
    }

    @Test
    void getVerificationStatusReturnsOkWhenStatusIsAvailable() throws Exception {
        when(verifierService.retrieveVerificationStatus("tx-id"))
                .thenReturn(new VerificationStatus("AVAILABLE", "tx-id"));

        mockMvc.perform(get("/verification-presentation/uniqueKey/status")
                        .sessionAttr("verification_transaction_data_uniqueKey", verificationTransactionData))
                .andExpect(status().isOk());
    }

    @Test
    void getVerificationStatusReturnsAcceptedWhenStatusIsWait() throws Exception {
        when(verifierService.retrieveVerificationStatus("tx-id"))
                .thenReturn(new VerificationStatus("WAIT", "tx-id"));

        mockMvc.perform(get("/verification-presentation/uniqueKey/status")
                        .sessionAttr("verification_transaction_data_uniqueKey", verificationTransactionData))
                .andExpect(status().isAccepted());
    }

    @Test
    void getVerificationStatusReturnsNotFoundWhenStatusIsUnknown() throws Exception {
        when(verifierService.retrieveVerificationStatus("tx-id"))
                .thenReturn(new VerificationStatus("UNKNOWN", "tx-id"));

        mockMvc.perform(get("/verification-presentation/uniqueKey/status")
                        .sessionAttr("verification_transaction_data_uniqueKey", verificationTransactionData))
                .andExpect(status().isNotFound());
    }

    @Test
    void getVerificationStatusReturnsNotFoundWhenStatusIsBlank() throws Exception {
        when(verifierService.retrieveVerificationStatus("tx-id"))
                .thenReturn(new VerificationStatus("", "tx-id"));

        mockMvc.perform(get("/verification-presentation/uniqueKey/status")
                        .sessionAttr("verification_transaction_data_uniqueKey", verificationTransactionData))
                .andExpect(status().isNotFound());
    }

    @Test
    void getVerificationStatusReturnsServerErrorForUnexpectedStatus() throws Exception {
        when(verifierService.retrieveVerificationStatus("tx-id"))
                .thenReturn(new VerificationStatus("SOMETHING_ELSE", "tx-id"));

        mockMvc.perform(get("/verification-presentation/uniqueKey/status")
                        .sessionAttr("verification_transaction_data_uniqueKey", verificationTransactionData))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void postVerificationStartWithBlankCredentialConfigurationIdFailsValidation() throws Exception {
        mockMvc.perform(post("/verification-start")
                        .param("credentialConfigurationId", "")
                        .param("selectedClaimPaths", "personidentifikator"))
                .andExpect(status().isOk())
                .andExpect(view().name("verification-start"))
                .andExpect(model().attributeHasFieldErrors("verificationForm", "credentialConfigurationId"))
                .andExpect(model().attributeDoesNotExist("verificationSuccessMessage"));
    }

    @Test
    void postVerificationStartWithBlankSelectedClaimsFailsValidation() throws Exception {
        mockMvc.perform(post("/verification-start")
                        .param("credentialConfigurationId", issuanceDefinitionId))
                .andExpect(status().isOk())
                .andExpect(view().name("verification-start"))
                .andExpect(model().attributeHasFieldErrors("verificationForm", "selectedClaimPaths"))
                .andExpect(model().attributeDoesNotExist("verificationSuccessMessage"));
    }

    @Test
    void postVerificationStartWithBothFieldsBlankFailsValidationOnBoth() throws Exception {
        mockMvc.perform(post("/verification-start")
                        .param("credentialConfigurationId", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("verification-start"))
                .andExpect(model().attributeHasFieldErrors("verificationForm", "credentialConfigurationId", "selectedClaimPaths"));
    }

    @Test
    void postVerificationStartReturnsCredentialDefinitionsOnValidationError() throws Exception {
        mockMvc.perform(post("/verification-start")
                        .param("credentialConfigurationId", ""))
                .andExpect(status().isOk())
                .andExpect(model().attribute("credentialDefinitions", hasSize(2)));
    }
}
