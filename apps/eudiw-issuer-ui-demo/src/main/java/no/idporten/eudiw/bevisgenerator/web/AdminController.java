package no.idporten.eudiw.bevisgenerator.web;

import jakarta.validation.Valid;
import no.idporten.eudiw.bevisgenerator.byob.CredentialService;
import no.idporten.eudiw.bevisgenerator.config.BevisgeneratorProperties;
import no.idporten.eudiw.bevisgenerator.exception.IssuerUiException;
import no.idporten.eudiw.bevisgenerator.integration.byobservice.model.CredentialDefinition;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.IssuerServerProperties;
import no.idporten.eudiw.bevisgenerator.web.models.AddCredentialForm;
import no.idporten.eudiw.bevisgenerator.web.models.CredentialDto;
import no.idporten.eudiw.bevisgenerator.web.models.EditCredentialForm;
import no.idporten.eudiw.bevisgenerator.web.models.advancedForm.ClaimForm;
import no.idporten.eudiw.bevisgenerator.web.models.advancedForm.CreateForm;
import no.idporten.eudiw.bevisgenerator.web.models.advancedForm.EditForm;
import no.idporten.eudiw.bevisgenerator.web.models.advancedForm.SimpleCredentialForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Controller
public class AdminController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final CredentialService credentialService;
    private final IssuerServerProperties properties;
    private final BevisgeneratorProperties bevisgeneratorProperties;
    private final ObjectMapper objectMapper;

    public AdminController(CredentialService credentialService, IssuerServerProperties properties, BevisgeneratorProperties bevisgeneratorProperties, ObjectMapper objectMapper) {
        this.credentialService = credentialService;
        this.properties = properties;
        this.bevisgeneratorProperties = bevisgeneratorProperties;
        this.objectMapper = objectMapper;
    }

    @ModelAttribute("allowBevisTyperV2")
    public boolean bevisTyperV2Enabled() {
        return bevisgeneratorProperties.getFeatureSwitches().isAllowBevistyperV2();
    }

    @ModelAttribute("issuerUrl")
    public String issuerUrl() {
        return properties.credentialIssuer();
    }

    @GetMapping("/admin")
    public ModelAndView admin() {
        return new ModelAndView("admin", "credentials", credentialService.getCredentialsForEdit());
    }

    @GetMapping("/add-credential")
    public ModelAndView addCredential() {
        CredentialDto emptyDto = credentialService.getEmptyCredentialDefinition();
        return new ModelAndView("add", "addCredentialForm", new AddCredentialForm(emptyDto.json()));
    }

    @PostMapping("/add-credential")
    public ModelAndView addNewCredential(@Valid AddCredentialForm addCredentialForm, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            // TODO: Add json validation
            logger.error("BindingResult errors: {}", bindingResult.getAllErrors());
            return new ModelAndView("add", "addCredentialForm", addCredentialForm);
        }

        credentialService.storeCredential(new CredentialDto(addCredentialForm.credentialType(), addCredentialForm.json()));

        return new ModelAndView("redirect:/admin", "credentials", credentialService.getCredentialsForEdit());
    }

    @GetMapping("/edit-credential/{credential_type}")
    public ModelAndView editCredential(@PathVariable("credential_type") String credentialType) {
        CredentialDto cd = credentialService.findCredential(credentialType);
        return new ModelAndView("edit", "editCredentialForm", new EditCredentialForm(credentialType, cd.json()));
    }

    @PostMapping("/edit-credential/{credential_type}")
    public ModelAndView editCredentialPost(
            @PathVariable("credential_type") String credentialType,
            @Valid EditCredentialForm editCredentialForm,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            // TODO: Add json validation
            logger.error("BindingResult errors: {}", bindingResult.getAllErrors());
            return new ModelAndView("add", "editCredentialForm", editCredentialForm);
        }

        logger.info("Editing credential with credentialType {}", credentialType);

        credentialService.editCredential(new CredentialDto(credentialType, editCredentialForm.json()));

        return new ModelAndView("redirect:/admin", "credentials", credentialService.getCredentialsForEdit());
    }

    /*
    @GetMapping("/delete-credential/{credential_configuration_id}")
    public ModelAndView deleteCredential(@PathVariable("credential_configuration_id") String credentialConfigurationId) {
        logger.info("Deleting credential with credentialType {}", credentialConfigurationId);

        credentialService.deleteCredential(credentialConfigurationId);
        return new ModelAndView("redirect:/admin", "credentials", credentialService.getCredentials());
    }*/


    @GetMapping("/add-credential-new")
    public ModelAndView showForm() {
        return new ModelAndView("add-new", "form", new SimpleCredentialForm());
    }

    @PostMapping("/add-credential-new")
    public ModelAndView submitForm(@Validated(CreateForm.class) @Valid @ModelAttribute("form") SimpleCredentialForm form,
                                   BindingResult bindingResult) {
        if (form.rawJson() != null && !form.rawJson().isBlank()) {

            SimpleCredentialForm resolved = resolveFromRawJson(form, bindingResult);
            if (bindingResult.hasErrors()) {
                return credentialFormWithErrors("add-new", form, bindingResult, null);
            }

            credentialService.storeCredential(resolved);

            return new ModelAndView("redirect:/admin");
        }

        if (bindingResult.hasErrors()) {
            logger.error("BindingResult errors: {}", bindingResult.getAllErrors());
            return credentialFormWithErrors("add-new", form, bindingResult, null);
        }

        credentialService.storeCredential(form);

        return new ModelAndView("redirect:/admin");
    }

    @GetMapping("/edit-credential-new/{credential_type}")
    public ModelAndView edit(@PathVariable("credential_type") String credentialType) {
        CredentialDefinition cd = credentialService.findCredentialDefinition(credentialType);
        SimpleCredentialForm form = new SimpleCredentialForm(cd);
        String credentialJson;

        try {
            credentialJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cd);
        } catch (JacksonException e) {
            logger.error("Failed to serialize CredentialDefinition for {}", credentialType, e);
            credentialJson = "{}";
        }

        ModelAndView mav = new ModelAndView("edit-new");
        mav.addObject("form", form);
        mav.addObject("credentialJson", credentialJson);

        return mav;
    }

    @PostMapping("/edit-credential-new/{credential_type}")
    public ModelAndView edit(@PathVariable("credential_type") String credentialType,
                             @Validated(EditForm.class) @Valid SimpleCredentialForm form,
                             BindingResult bindingResult) {
        if (form.rawJson() != null && !form.rawJson().isBlank()) {

            SimpleCredentialForm resolved = resolveFromRawJson(form, bindingResult);
            if (bindingResult.hasErrors()) {
                return credentialFormWithErrors("edit-new", form, bindingResult, credentialType);
            }

            credentialService.editCredential(new SimpleCredentialForm(credentialType, resolved.format(), resolved.scope(), resolved.name(), resolved.claims(), resolved.backgroundColor(), resolved.textColor(), null));

            return new ModelAndView("redirect:/admin");
        }

        if (bindingResult.hasErrors()) {
            logger.error("BindingResult errors: {}", bindingResult.getAllErrors());
            return credentialFormWithErrors("edit-new", form, bindingResult, credentialType);
        }

        credentialService.editCredential(new SimpleCredentialForm(credentialType, form.format(), form.scope(), form.name(), form.claims(), form.backgroundColor(), form.textColor(), null));

        return new ModelAndView("redirect:/admin");
    }

    private ModelAndView credentialFormWithErrors(
            String viewName,
            SimpleCredentialForm form,
            BindingResult bindingResult,
            String editCredentialType
    ) {
        ModelAndView modelAndView = new ModelAndView(viewName, "form", form)
                .addObject(BindingResult.MODEL_KEY_PREFIX + "form", bindingResult);
        if (editCredentialType != null) {
            modelAndView.addObject("editCredentialType", editCredentialType);
        }

        if (form.rawJson() != null && !form.rawJson().isBlank()) {
            return modelAndView.addObject("credentialJson", form.rawJson());
        }

        try {
            String credentialJson = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(new CredentialDefinition(form));
            return modelAndView.addObject("credentialJson", credentialJson);
        } catch (JacksonException e) {
            throw new IssuerUiException("Failed to serialize invalid credential form", e);
        }
    }

    /**
     * Parses rawJson from the form, maps claims, and returns a resolved SimpleCredentialForm.
     * Adds a binding error and returns the original form if JSON is invalid.
     */
    private SimpleCredentialForm resolveFromRawJson(SimpleCredentialForm form, BindingResult bindingResult) {
        try {
            JsonNode root = objectMapper.readTree(form.rawJson());

            String name = form.name();
            String backgroundColor = form.backgroundColor();
            String textColor = form.textColor();
            JsonNode displayArr = root.path("credential_metadata").path("display");
            if (displayArr.isArray() && !displayArr.isEmpty()) {
                JsonNode displayNode = displayArr.get(0);
                JsonNode nameNode = displayNode.path("name");
                if (!nameNode.isMissingNode()) {
                    name = nameNode.asText(form.name());
                }
                JsonNode backgroundColorNode = displayNode.path("background_color");
                if (!backgroundColorNode.isMissingNode()) {
                    backgroundColor = backgroundColorNode.asText(form.backgroundColor());
                }
                JsonNode textColorNode = displayNode.path("text_color");
                if (!textColorNode.isMissingNode()) {
                    textColor = textColorNode.asText(form.textColor());
                }
            }

            List<ClaimForm> claims = parseClaimsFromJson(root);
            return new SimpleCredentialForm(form.credentialType(), form.format(), form.scope(), name, claims, backgroundColor, textColor, null);
        } catch (JacksonException e) {
            bindingResult.reject("rawJson.invalid", "Ugyldig JSON: " + e.getMessage());
            return form;
        }
    }

    private List<ClaimForm> parseClaimsFromJson(JsonNode root) {
        List<ClaimForm> claims = new ArrayList<>();
        JsonNode exampleData = root.path("example_credential_data");
        JsonNode claimsArray = root.path("credential_metadata").path("claims");

        if (!claimsArray.isArray()) {
            return claims;
        }

        for (JsonNode claimNode : claimsArray) {
            String path = claimNode.path("path").asText("");
            String displayName = extractClaimDisplayName(claimNode, path);
            String type = extractClaimType(claimNode);
            String exampleValue = extractClaimExampleValue(exampleData, path);
            claims.add(new ClaimForm(path, displayName, type, null, exampleValue));
        }

        return claims;
    }

    private String extractClaimDisplayName(JsonNode claimNode, String defaultValue) {
        JsonNode claimDisplay = claimNode.path("display");
        if (claimDisplay.isArray() && !claimDisplay.isEmpty()) {
            return claimDisplay.get(0).path("name").asText(defaultValue);
        }
        return defaultValue;
    }

    private String extractClaimType(JsonNode claimNode) {
        return claimNode.has("value_type") ? claimNode.path("value_type").asText("string") : "string";
    }

    private String extractClaimExampleValue(JsonNode exampleData, String path) {
        if (exampleData.isMissingNode() || !exampleData.has(path)) {
            return "";
        }
        JsonNode val = exampleData.get(path);
        return val.isTextual() ? val.asText() : val.toString();
    }

}
