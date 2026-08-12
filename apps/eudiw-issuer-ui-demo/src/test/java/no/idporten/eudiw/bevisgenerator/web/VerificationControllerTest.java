package no.idporten.eudiw.bevisgenerator.web;

import no.idporten.eudiw.bevisgenerator.integration.issuerserver.IssuerServerService;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.IssuerServerProperties;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.credentialdefinitionmodel.ClaimMetadata;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.credentialdefinitionmodel.CredentialConfiguration;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.credentialdefinitionmodel.CredentialConfigurationMetadata;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.credentialdefinitionmodel.CredentialIssuerMetadata;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.DCQLService;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.DCQLServiceImpl;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.VerifierService;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerificationResult;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerificationStartResponse;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerificationTransactionData;
import no.idporten.eudiw.bevisgenerator.integration.byobservice.model.Display;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.servlet.view.RedirectView;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.argThat;
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
    private String issuanceDefinitionId;

    @BeforeEach
    void setUp() {
        IssuerServerService issuerServerService = mock(IssuerServerService.class);
        IssuerServerProperties issuerServerProperties = mock(IssuerServerProperties.class);
        verifierService = mock(VerifierService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        DCQLService dcqlService = new DCQLServiceImpl(objectMapper);

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
                        List.of(new VerificationResult.CredentialPresentation(
                                Map.of("age_over_18", true)
                        ))
                )
        ));

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new VerificationController(issuerServerService, issuerServerProperties, verifierService, objectMapper, dcqlService))
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
                .andExpect(redirectedUrl("/verification-presentation"))
                .andExpect(flash().attributeExists("qrCode"))
                .andExpect(flash().attributeExists("authorizationRequest"))
                .andExpect(flash().attribute("transactionId", "tx-id"))
                .andExpect(flash().attribute("requestBody", """
                        {
                          "dcql_query" : {
                            "credentials" : [ ]
                          }
                        }"""));

        verify(verifierService).startVerification(argThat(dcql ->
                dcql.contains("\"id\"")
                        && dcql.contains("\"format\":\"dc+sd-jwt\"")
                        && !dcql.contains("\"personidentifikator\"")
                        && dcql.contains("\"epostadresse\"")
        ));
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
