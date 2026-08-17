package no.idporten.eudiw.bevisgenerator.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import no.idporten.eudiw.bevisgenerator.exception.IssuerUiException;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.IssuerServerService;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.IssuerServerProperties;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.DCQLService;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.VerifierService;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.CredentialDefinitionDisplayData;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerificationResult;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerificationStatus;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerificationTransactionData;
import no.idporten.eudiw.bevisgenerator.web.models.StartVerificationForm;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    public ModelAndView startVerification(
            @Valid @ModelAttribute("verificationForm")
            StartVerificationForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpSession session
    ) {
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

        String verificationId = UUID.randomUUID().toString();
        String requestBody = buildStartVerificationRequestBody(credentialDefinition, form.selectedClaimPaths(), verificationId);

        VerificationTransactionData verificationTransactionData = verifierService.startVerification(requestBody);

        session.setAttribute(getVerificationTransactionKey(verificationId), verificationTransactionData);

        return new ModelAndView("redirect:/verification-presentation/" + verificationId);
    }

    @GetMapping("/verification-presentation/{verification-id}")
    public ModelAndView verificationPresentation(@PathVariable("verification-id") String verificationId, HttpSession session) {
        VerificationTransactionData verificationTransactionData = (VerificationTransactionData) session.getAttribute(getVerificationTransactionKey(verificationId));

        if (verificationTransactionData == null) {
            throw new IssuerUiException("Missing verification transaction data for verificationId: " + verificationId);
        }

        return new ModelAndView("verification-presentation")
                .addObject("verificationId", verificationId)
                .addObject("qrCode", verificationTransactionData.verificationStartResponse().authorizationRequestQrCode())
                .addObject("authorizationRequest", verificationTransactionData.verificationStartResponse().authorizationRequest())
                .addObject("transactionId", verificationTransactionData.verificationStartResponse().verifierTransactionId())
                .addObject("statusUri", verificationTransactionData.statusUri())
                .addObject("requestBody", toJsonString(verificationTransactionData.requestBody()))
                .addObject("requestUri", verificationTransactionData.requestUri())
                .addObject("responseBody", toJsonString(verificationTransactionData.verificationStartResponse()));
    }

    @GetMapping("/verification-result/{verification-id}")
    public ModelAndView verificationResult(@PathVariable("verification-id") String verificationId, HttpSession session) {
        if (verificationId == null || verificationId.isBlank()) {
            throw new IssuerUiException("Missing verificationId");
        }

        String transactionId = getTransactionIdFromSession(verificationId, session);

        session.removeAttribute(getVerificationTransactionKey(verificationId));

        VerificationResult result = verifierService.retrieveVerificationResult(transactionId);

        return new ModelAndView("verification-result")
                .addObject("result", result)
                .addObject("resultJson", toJsonString(result.credentials()));
    }

    @GetMapping("/verification-presentation/{verification-id}/status")
    public ResponseEntity<?> verificationStatus(@PathVariable("verification-id") String verificationId, HttpSession session) {
        if (verificationId == null || verificationId.isBlank()) {
            throw new IssuerUiException("Missing verificationId");
        }

        String transactionId = getTransactionIdFromSession(verificationId, session);
        VerificationStatus verificationStatus = verifierService.retrieveVerificationStatus(transactionId);
        String status = verificationStatus.status();

        if (status.isBlank() || status.equals("UNKNOWN")) {
            return ResponseEntity.notFound().build();
        }
        if (status.equals("WAIT")) {
            return ResponseEntity.accepted().build();
        }
        if (status.equals("AVAILABLE")) {
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.internalServerError().build();
    }

    private static String getTransactionIdFromSession(String verificationId, HttpSession session) {
        VerificationTransactionData verificationTransactionData = (VerificationTransactionData) session.getAttribute(getVerificationTransactionKey(verificationId));
        if (verificationTransactionData == null) {
            throw new IssuerUiException("Missing verification transaction data for verificationId=" + verificationId);
        }
        return verificationTransactionData.verificationStartResponse().verifierTransactionId();
    }

    private static String getVerificationTransactionKey(String verificationId) {
        return "verification_transaction_data_%s".formatted(verificationId);
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

    private String buildStartVerificationRequestBody(CredentialDefinitionDisplayData credentialDefinition, List<String> selectedClaimPaths, String verificationId) {
        Map<String, Object> dcql = dcqlService.buildDcqlMap(credentialDefinition, selectedClaimPaths);

        String redirectUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/verification-presentation/{verification-id}")
                .buildAndExpand(verificationId)
                .toString();

        return toJsonString(Map.of(
                "dcql_query", dcql,
                "redirect_uri", redirectUri
        ), false);
    }

    private String toJsonString(Object object) {
       return toJsonString(object, true);
    }

    private String toJsonString(Object object, boolean pretty) {
        try {
            if (object instanceof String) {
                object = objectMapper.readTree((String) object);
            }
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
