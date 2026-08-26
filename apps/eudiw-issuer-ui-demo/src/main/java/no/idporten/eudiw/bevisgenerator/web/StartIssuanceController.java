package no.idporten.eudiw.bevisgenerator.web;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import no.idporten.eudiw.bevisgenerator.config.BevisgeneratorProperties;
import no.idporten.eudiw.bevisgenerator.exception.IssuerUiException;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.IssuerServerService;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.CredentialConfiguration;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.IssuerServerProperties;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.domain.IssuanceResponse;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.domain.IssuanceStatusResponse;
import no.idporten.eudiw.bevisgenerator.web.models.StartIssuanceForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Controller
public class StartIssuanceController {

    private static final String ISSUANCE_CONFIGURATION_SESSION_KEY = "issuance_credential_configuration_%s";
    private static final String ISSUANCE_COMPLETED_SESSION_KEY = "issuance_completed_%s";
    private static final String ISSUANCE_DESCRIPTION_SESSION_KEY = "issuance_credential_description_%s";
    private static final String ISSUANCE_SUBJECT_IDENTIFIER_SESSION_KEY = "issuance_subject_identifier_%s";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Logger logger = LoggerFactory.getLogger(StartIssuanceController.class);

    private final IssuerServerService issuerServerService;

    private final IssuerServerProperties properties;
    private final BevisgeneratorProperties bevisgeneratorProperties;


    @Autowired
    public StartIssuanceController(IssuerServerService issuerServerService,
                                   IssuerServerProperties properties,
                                   BevisgeneratorProperties bevisgeneratorProperties) {
        this.issuerServerService = issuerServerService;
        this.properties = properties;
        this.bevisgeneratorProperties = bevisgeneratorProperties;
    }

    @ModelAttribute("issuerUrl")
    public String issuerUrl() {
        return properties.credentialIssuer();
    }

    @ModelAttribute("allowVerification")
    public boolean verificationEnabled() {
        return bevisgeneratorProperties.getFeatureSwitches().isAllowVerification();
    }

    @GetMapping("/")
    public String newIndex(Model model) {
        return "index";
    }

    @GetMapping("/issue")
    public ModelAndView issue() {
        return new ModelAndView("issue", "credential_configurations", issuerServerService.getAll());
    }

    @GetMapping("/start-issuance/{credential_configuration_id}")
    public String start(
            @PathVariable("credential_configuration_id") String credentialConfigurationId,
            Model model) {
        CredentialConfiguration credentialConfiguration = issuerServerService.getById(credentialConfigurationId);
        model.addAttribute("credentialConfiguration", credentialConfiguration);
        model.addAttribute("startIssuanceForm", new StartIssuanceForm(credentialConfiguration.jsonRequest(), credentialConfiguration.personIdentifier()));
        return "start";
    }

    @PostMapping("/start-issuance/{credential_configuration_id}")
    public String startIssuance(@PathVariable("credential_configuration_id") String credentialConfigurationId,
                                @ModelAttribute("startIssuanceForm") StartIssuanceForm startIssuanceForm,
                                Model model,
                                HttpSession session) {
        CredentialConfiguration credentialConfiguration = issuerServerService.getById(credentialConfigurationId);
        String normalizedJson = startIssuanceForm.json().replaceAll("\\s", ""); // TODO add validation
        logger.info(normalizedJson);

        model.addAttribute("request", createRequestTrace(credentialConfiguration, startIssuanceForm));

        IssuanceResponse response = issuerServerService.startIssuance(credentialConfiguration, startIssuanceForm);

        String uri = convertToCredentialOfferUri(response);
        String qrCode = null;
        try {
            qrCode = Base64.getEncoder().encodeToString(createQRCodeImage(uri));
        } catch (IOException | WriterException e) {
            logger.error("Failed to create QRCode for uri=" + uri, e);
            model.addAttribute("error", "Generering av QR kode feila.");
        }

        Issuance issuance = new Issuance(toPrettyJsonString(response), uri, qrCode);
        model.addAttribute("issuance", issuance);
        model.addAttribute("issuedCredentialConfigurationId", credentialConfiguration.credentialConfigurationId());
        model.addAttribute("issuedTransactionId", response.issuanceTransactionId());
        session.setAttribute(
                ISSUANCE_CONFIGURATION_SESSION_KEY.formatted(response.issuanceTransactionId()),
                credentialConfiguration.credentialConfigurationId()
        );
        session.setAttribute(
                ISSUANCE_DESCRIPTION_SESSION_KEY.formatted(response.issuanceTransactionId()),
                credentialConfiguration.description()
        );
        if (StringUtils.hasText(startIssuanceForm.personIdentifier())
                && issuerServerService.getSubjectCredentialConfigurationById(
                        credentialConfiguration.credentialConfigurationId()) != null) {
            session.setAttribute(
                    ISSUANCE_SUBJECT_IDENTIFIER_SESSION_KEY.formatted(response.issuanceTransactionId()),
                    startIssuanceForm.personIdentifier()
            );
        }
        return "issuer_response";
    }

    @GetMapping("/issuance/{issuance-transaction-id}/status")
    public ResponseEntity<Void> issuanceStatus(
            @PathVariable("issuance-transaction-id") String issuanceTransactionId,
            HttpSession session
    ) {
        String credentialConfigurationId = getCredentialConfigurationId(issuanceTransactionId, session);
        CredentialConfiguration credentialConfiguration = issuerServerService.getById(credentialConfigurationId);
        IssuanceStatusResponse issuanceStatus = issuerServerService.retrieveIssuanceStatus(
                credentialConfiguration,
                issuanceTransactionId
        );

        return switch (issuanceStatus.status()) {
            case OFFER_ISSUED -> ResponseEntity.accepted().build();
            case CREDENTIAL_ISSUED, CREDENTIAL_ACCEPTED -> {
                session.setAttribute(ISSUANCE_COMPLETED_SESSION_KEY.formatted(issuanceTransactionId), true);
                yield ResponseEntity.ok().build();
            }
            case CREDENTIAL_FAILURE, CREDENTIAL_DELETED -> ResponseEntity.unprocessableContent().build();
            case UNKNOWN -> ResponseEntity.notFound().build();
            case UNRECOGNIZED -> ResponseEntity.internalServerError().build();
        };
    }

    @GetMapping("/issuance/{issuance-transaction-id}/complete")
    public String issuanceComplete(
            @PathVariable("issuance-transaction-id") String issuanceTransactionId,
            HttpSession session,
            HttpServletResponse response,
            Model model
    ) {
        String credentialConfigurationId = getCredentialConfigurationId(issuanceTransactionId, session);
        if (!Boolean.TRUE.equals(
                session.getAttribute(ISSUANCE_COMPLETED_SESSION_KEY.formatted(issuanceTransactionId)))) {
            throw new IssuerUiException(
                    "Issuance is not completed for issuance_transaction_id=" + issuanceTransactionId
            );
        }

        response.setHeader("Cache-Control", "no-store, private");
        response.setHeader("Referrer-Policy", "no-referrer");

        model.addAttribute("issuedCredentialConfigurationId", credentialConfigurationId);
        model.addAttribute(
                "issuedCredentialDescription",
                getCredentialDescription(issuanceTransactionId, credentialConfigurationId, session)
        );
        model.addAttribute("issuedTransactionId", issuanceTransactionId);
        model.addAttribute("issuedSubjectIdentifier", getSubjectIdentifier(issuanceTransactionId, session));
        return "issuance-complete";
    }

    private static String getCredentialConfigurationId(String issuanceTransactionId, HttpSession session) {
        Object credentialConfigurationId = session.getAttribute(
                ISSUANCE_CONFIGURATION_SESSION_KEY.formatted(issuanceTransactionId)
        );
        if (!(credentialConfigurationId instanceof String id) || id.isBlank()) {
            throw new IssuerUiException(
                    "Missing issuance transaction data for issuance_transaction_id=" + issuanceTransactionId
            );
        }
        return id;
    }

    private static String getSubjectIdentifier(String issuanceTransactionId, HttpSession session) {
        Object subjectIdentifier = session.getAttribute(
                ISSUANCE_SUBJECT_IDENTIFIER_SESSION_KEY.formatted(issuanceTransactionId)
        );
        return subjectIdentifier instanceof String id ? id : "";
    }

    private static String getCredentialDescription(
            String issuanceTransactionId,
            String credentialConfigurationId,
            HttpSession session
    ) {
        Object description = session.getAttribute(
                ISSUANCE_DESCRIPTION_SESSION_KEY.formatted(issuanceTransactionId)
        );
        return description instanceof String value && StringUtils.hasText(value)
                ? value
                : credentialConfigurationId;
    }

    private IssuanceRequest createRequestTrace(CredentialConfiguration credentialConfiguration, StartIssuanceForm startIssuanceForm) {
        String contentType = "Content-Type: " + MediaType.APPLICATION_JSON;
        String authorization = "Authorization: Bearer [Maskinporten-token]";
        return new IssuanceRequest(startIssuanceForm.json(), credentialConfiguration.credentialIssuer() + properties.issuanceEndpoint(), authorization, contentType);
    }

    private String convertToCredentialOfferUri(IssuanceResponse response) {
        String jsonString = toJsonString(response);
        String offerEncoded = URLEncoder.encode(jsonString, StandardCharsets.UTF_8);
        String uri = "openid-credential-offer://?credential_offer=" + offerEncoded;
        logger.info("Issuer offer: " + response);
        logger.info("Issuer offer encoded: " + offerEncoded);
        return uri;
    }

    private String toJsonString(IssuanceResponse response) {
        try {
            return objectMapper.writeValueAsString(response.credentialOffer());
        } catch (JacksonException e) {
            throw new IssuerUiException("Failed to convert response to Json string", e);
        }
    }

    private String toPrettyJsonString(IssuanceResponse response) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        } catch (JacksonException e) {
            throw new IssuerUiException("Failed to convert response to pretty Json string", e);
        }
    }

    private byte[] createQRCodeImage(String text) throws IOException, WriterException {
        int width = 200;
        int height = 200;
        BitMatrix bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height);
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        return pngOutputStream.toByteArray();
    }

}
