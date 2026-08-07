package no.idporten.eudiw.bevisgenerator.web;

import jakarta.validation.Valid;
import no.idporten.eudiw.bevisgenerator.exception.IssuerUiException;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.IssuerServerService;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.IssuerServerProperties;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.DCQLService;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.VerifierService;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.CredentialDefinitionDisplayData;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerificationResult;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerificationTransactionData;
import no.idporten.eudiw.bevisgenerator.web.models.StartVerificationForm;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Controller
public class VerificationController {

    private final IssuerServerService issuerServerService;
    private final IssuerServerProperties properties;
    private final VerifierService verifierService;
    private final ObjectMapper objectMapper;
    private final DCQLService dcqlService;

    public VerificationController(
            IssuerServerService issuerServerService,
            IssuerServerProperties properties,
            VerifierService verifierService,
            ObjectMapper objectMapper, DCQLService dcqlService
    ) {
        this.issuerServerService = issuerServerService;
        this.properties = properties;
        this.verifierService = verifierService;
        this.objectMapper = objectMapper;
        this.dcqlService = dcqlService;
    }

    @ModelAttribute("issuerUrl")
    public String issuerUrl() {
        return properties.credentialIssuer();
    }

    @GetMapping("/verification-start")
    public ModelAndView verify() {
        return baseView(new StartVerificationForm());
    }

    @PostMapping("/verification-start")
    public ModelAndView startVerification(@Valid @ModelAttribute("verificationForm") StartVerificationForm form,
                                          BindingResult bindingResult,
                                          RedirectAttributes redirectAttributes) {
        List<CredentialDefinitionDisplayData> credentialDefinitions = dcqlService.createCredentialDefinitionDisplayData(
                issuerServerService.getAllCredentialIssuerMetadata()
        );

        if (bindingResult.hasErrors()) {
            return baseView(form, credentialDefinitions);
        }

        CredentialDefinitionDisplayData credentialDefinition = credentialDefinitions.stream()
                .filter(definition -> definition.id().equals(form.credentialConfigurationId()))
                .findFirst()
                .orElse(null);

        if (credentialDefinition == null) {
            bindingResult.rejectValue(
                    "credentialConfigurationId",
                    "credentialConfigurationId.invalid",
                    "Ukjend credential configuration"
            );

            return baseView(form, credentialDefinitions);
        }

        String dcql = dcqlService.buildDcql(credentialDefinition, form.selectedClaimPaths());
        VerificationTransactionData verificationTransactionData = verifierService.startVerification(dcql);

        redirectAttributes.addFlashAttribute("qrCode", verificationTransactionData.verificationStartResponse().authorizationRequestQrCode());
        redirectAttributes.addFlashAttribute("authorizationRequest", verificationTransactionData.verificationStartResponse().authorizationRequest());
        redirectAttributes.addFlashAttribute("transactionId", verificationTransactionData.verificationStartResponse().verifierTransactionId());
        redirectAttributes.addFlashAttribute("statusUri", verificationTransactionData.statusUri());

        return new ModelAndView("redirect:/verification-presentation");
    }

    @GetMapping("/verification-presentation")
    public ModelAndView verificationPresentation() {
        return new ModelAndView("verification-presentation");
    }

    @GetMapping("/verification-result")
    public ModelAndView verificationResult(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IssuerUiException("Missing transactionId");
        }

        VerificationResult result = verifierService.retrieveVerificationResult(transactionId);
        return new ModelAndView("verification-result")
                .addObject("result", result)
                .addObject("resultJson", toJsonString(result.credentials(), true));
    }

    private ModelAndView baseView(StartVerificationForm form) {
        List<CredentialDefinitionDisplayData> credentialDefinitions = dcqlService.createCredentialDefinitionDisplayData(
                issuerServerService.getAllCredentialIssuerMetadata()
        );
        return baseView(form, credentialDefinitions);
    }

    private ModelAndView baseView(StartVerificationForm form, List<CredentialDefinitionDisplayData> credentialDefinitions) {
        return new ModelAndView("verification-start")
                .addObject("verificationForm", form)
                .addObject("credentialDefinitions", credentialDefinitions)
                .addObject("credentialDefinitionsJson", toJsonString(credentialDefinitions, false))
                .addObject("selectedClaimPathsJson", toJsonString(form.selectedClaimPaths(), false));
    }

    private String toJsonString(Object object, boolean pretty) {
        try {
            String text = pretty
                    ? objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object)
                    : objectMapper.writeValueAsString(object);

            return sanitizeForHtmlScriptTag(text);
        } catch (JacksonException e) {
            throw new IssuerUiException("Failed to convert object to Json string", e);
        }
    }

    private String sanitizeForHtmlScriptTag(String json) {
        return json
                .replace("&", "\\u0026")
                .replace("<", "\\u003c")
                .replace(">", "\\u003e")
                .replace("\u2028", "\\u2028")
                .replace("\u2029", "\\u2029");
    }
}
