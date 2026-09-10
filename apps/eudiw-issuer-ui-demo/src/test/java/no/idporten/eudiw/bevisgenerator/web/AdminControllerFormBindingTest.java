package no.idporten.eudiw.bevisgenerator.web;

import no.idporten.eudiw.bevisgenerator.byob.CredentialService;
import no.idporten.eudiw.bevisgenerator.config.BevisgeneratorProperties;
import no.idporten.eudiw.bevisgenerator.config.FeatureSwitches;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.IssuerServerProperties;
import no.idporten.eudiw.bevisgenerator.web.models.CredentialDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceView;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Exercises the real Spring MVC data binder (unlike AdminControllerTest, which calls the
 * controller methods directly). This is the only test level that can catch bugs where the
 * submitted HTML form doesn't actually carry the "claims" request parameters the bean
 * validation on SimpleCredentialForm requires - e.g. disabling the claims inputs client-side
 * before submit, which previously caused every save to fail with "Beviset må ha minimum 1.
 * claim" regardless of whether rawJson was populated.
 */
class AdminControllerFormBindingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CredentialService credentialService = mock(CredentialService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        IssuerServerProperties issuerServerProperties = mock(IssuerServerProperties.class);
        when(issuerServerProperties.credentialIssuer()).thenReturn("http://issuer");

        BevisgeneratorProperties bevisgeneratorProperties = mock(BevisgeneratorProperties.class);
        FeatureSwitches featureSwitches = mock(FeatureSwitches.class);
        when(bevisgeneratorProperties.getFeatureSwitches()).thenReturn(featureSwitches);

        AdminController controller = new AdminController(
                credentialService,
                issuerServerProperties,
                bevisgeneratorProperties,
                objectMapper
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers((viewName, locale) -> {
                    InternalResourceView view = new InternalResourceView();
                    view.setUrl("/templates/" + viewName + ".html");
                    return view;
                })
                .build();
    }

    @Test
    void storesCredentialWhenClaimsAndRawJsonAreBothSubmitted() throws Exception {
        String rawJson = """
                {
                  "credential_type": "studentbevis",
                  "format": "dc+sd-jwt",
                  "scope": "eudiw:eidas2sandkasse:dynamicvc",
                  "credential_metadata": {
                    "display": [{"name": "Studentbevis", "locale": "no"}],
                    "claims": [{"path": "student_id", "type": "string", "mandatory": true, "display": [{"name": "Studentnummer", "locale": "no"}]}]
                  },
                  "example_credential_data": {"student_id": "12345"}
                }
                """;

        // Uses the edit endpoint (EditForm validation group) rather than add-credential-new,
        // since the create-path also validates @UniqueCredentialType, which needs a
        // Spring-managed ConstraintValidatorFactory not available under standaloneSetup.
        mockMvc.perform(post("/admin/edit-credential-new/studentbevis")
                        .param("credentialType", "studentbevis")
                        .param("format", "dc+sd-jwt")
                        .param("scope", "eudiw:eidas2sandkasse:dynamicvc")
                        .param("name", "Studentbevis")
                        .param("claims[0].path", "student_id")
                        .param("claims[0].name", "Studentnummer")
                        .param("claims[0].exampleValue", "12345")
                        .param("rawJson", rawJson))
                .andExpect(view().name("redirect:/admin"));

        ArgumentCaptor<CredentialDto> captor = ArgumentCaptor.forClass(CredentialDto.class);
        verify(credentialService).editCredential(captor.capture());
    }
}
