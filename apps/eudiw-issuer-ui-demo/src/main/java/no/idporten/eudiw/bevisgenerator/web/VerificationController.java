package no.idporten.eudiw.bevisgenerator.web;

import jakarta.validation.Valid;
import no.idporten.eudiw.bevisgenerator.exception.IssuerUiException;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.IssuerServerService;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.CredentialConfiguration;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.IssuerServerProperties;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.VerifierService;
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

import java.util.ArrayList;
import java.util.List;

@Controller
public class VerificationController {

    private final IssuerServerService issuerServerService;
    private final IssuerServerProperties properties;
    private final VerifierService verifierService;
    private final ObjectMapper objectMapper;

    public VerificationController(
            IssuerServerService issuerServerService,
            IssuerServerProperties properties,
            VerifierService verifierService,
            ObjectMapper objectMapper
    ) {
        this.issuerServerService = issuerServerService;
        this.properties = properties;
        this.verifierService = verifierService;
        this.objectMapper = objectMapper;
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

        if (bindingResult.hasErrors()) {
            return baseView(form);
        }

        VerificationTransactionData verificationTransactionData = verifierService.startVerification(form.dcql());

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
                .addObject("resultJson", toPrettyJsonString(result));
    }

    private ModelAndView baseView(StartVerificationForm form) {
        List<CredentialConfiguration> credentialConfigurations = new ArrayList<>(issuerServerService.getAll());
        credentialConfigurations.addAll(issuerServerService.getAllSubjectCredentialConfigurations());
        return new ModelAndView("verification-start")
                .addObject("verificationForm", form)
                .addObject("credentialConfigurations", credentialConfigurations);
    }

    private String toPrettyJsonString(VerificationResult result) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result.credentials());
        } catch (JacksonException e) {
            throw new IssuerUiException("Failed to convert verification result to pretty Json string", e);
        }
    }
}
