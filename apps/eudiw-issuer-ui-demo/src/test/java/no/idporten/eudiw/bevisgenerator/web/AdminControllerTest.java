package no.idporten.eudiw.bevisgenerator.web;

import no.idporten.eudiw.bevisgenerator.byob.CredentialService;
import no.idporten.eudiw.bevisgenerator.config.BevisgeneratorProperties;
import no.idporten.eudiw.bevisgenerator.integration.byobservice.model.CredentialDefinition;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.IssuerServerProperties;
import no.idporten.eudiw.bevisgenerator.web.models.advancedForm.ClaimForm;
import no.idporten.eudiw.bevisgenerator.web.models.advancedForm.SimpleCredentialForm;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindingResult;
import org.springframework.validation.BeanPropertyBindingResult;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class AdminControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AdminController controller = new AdminController(
            mock(CredentialService.class),
            mock(IssuerServerProperties.class),
            mock(BevisgeneratorProperties.class),
            objectMapper
    );

    @Test
    void retainsSubmittedCredentialDataWhenValidationFails() {
        SimpleCredentialForm form = new SimpleCredentialForm(
                "studentbevis",
                "dc+sd-jwt",
                "eudiw:eidas2sandkasse:dynamicvc",
                "Studentbevis",
                List.of(new ClaimForm("student_id", "Studentnummer", "12345")),
                "#123456",
                "#abcdef",
                null
        );
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        bindingResult.rejectValue("credentialType", "duplicate", "Finnes allerede");

        var result = controller.submitForm(form, bindingResult);
        CredentialDefinition persistedForm = objectMapper.readValue(
                (String) result.getModel().get("credentialJson"),
                CredentialDefinition.class
        );

        assertEquals("add-new", result.getViewName());
        assertEquals(form, result.getModel().get("form"));
        assertEquals(bindingResult, result.getModel().get(BindingResult.MODEL_KEY_PREFIX + "form"));
        assertEquals("studentbevis", persistedForm.getCredentialType());
        assertEquals("Studentbevis", persistedForm.getCredentialMetadata().display().getFirst().name());
        assertEquals("#123456", persistedForm.getCredentialMetadata().display().getFirst().backgroundColor());
        assertEquals("#abcdef", persistedForm.getCredentialMetadata().display().getFirst().textColor());
        assertEquals("12345", persistedForm.getExampleCredentialData().get("student_id"));
    }

    @Test
    void retainsSubmittedRawJsonWhenValidationFails() {
        String rawJson = """
                {"credential_type":"studentbevis","format":"dc+sd-jwt","scope":"eudiw:eidas2sandkasse:dynamicvc"}
                """;
        SimpleCredentialForm form = new SimpleCredentialForm(
                "studentbevis",
                "dc+sd-jwt",
                "eudiw:eidas2sandkasse:dynamicvc",
                "Studentbevis",
                List.of(),
                null,
                null,
                rawJson
        );
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        bindingResult.rejectValue("credentialType", "duplicate", "Finnes allerede");

        var result = controller.submitForm(form, bindingResult);

        assertEquals(rawJson, result.getModel().get("credentialJson"));
    }

    @Test
    void retainsEditedCredentialDataWhenValidationFails() throws Exception {
        SimpleCredentialForm form = new SimpleCredentialForm(
                "studentbevis",
                "dc+sd-jwt",
                "eudiw:eidas2sandkasse:dynamicvc",
                "Oppdatert studentbevis",
                List.of(new ClaimForm("student_id", "Studentnummer", "12345")),
                "#654321",
                "#fedcba",
                null
        );
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
        bindingResult.rejectValue("name", "invalid", "Ugyldig navn");

        var result = controller.edit("studentbevis", form, bindingResult);
        CredentialDefinition persistedForm = objectMapper.readValue(
                (String) result.getModel().get("credentialJson"),
                CredentialDefinition.class
        );

        assertEquals("edit-new", result.getViewName());
        assertEquals(form, result.getModel().get("form"));
        assertEquals(bindingResult, result.getModel().get(BindingResult.MODEL_KEY_PREFIX + "form"));
        assertEquals("studentbevis", result.getModel().get("editCredentialType"));
        assertEquals("Oppdatert studentbevis", persistedForm.getCredentialMetadata().display().getFirst().name());
        assertEquals("#654321", persistedForm.getCredentialMetadata().display().getFirst().backgroundColor());
        assertEquals("#fedcba", persistedForm.getCredentialMetadata().display().getFirst().textColor());
        assertEquals("12345", persistedForm.getExampleCredentialData().get("student_id"));
    }
}
