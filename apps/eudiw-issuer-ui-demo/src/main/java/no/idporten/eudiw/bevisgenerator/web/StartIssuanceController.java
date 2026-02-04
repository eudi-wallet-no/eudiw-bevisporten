package no.idporten.eudiw.bevisgenerator.web;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import no.idporten.eudiw.bevisgenerator.exception.IssuerUiException;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.IssuerServerService;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.CredentialConfiguration;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.IssuerServerProperties;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.domain.IssuanceResponse;
import no.idporten.eudiw.bevisgenerator.web.models.StartIssuanceForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Logger logger = LoggerFactory.getLogger(StartIssuanceController.class);

    private final IssuerServerService issuerServerService;

    private final IssuerServerProperties properties;


    @Autowired
    public StartIssuanceController(IssuerServerService issuerServerService, IssuerServerProperties properties) {
        this.issuerServerService = issuerServerService;
        this.properties = properties;
    }

    @ModelAttribute("issuerUrl")
    public String issuerUrl() {
        return properties.credentialIssuer();
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
                                Model model) {
        CredentialConfiguration credentialConfiguration = issuerServerService.getById(credentialConfigurationId);
        String normalizedJson = startIssuanceForm.json().replaceAll("\\s", ""); // TODO add validation
        logger.info(normalizedJson);

        model.addAttribute("request", createRequestTraceing(startIssuanceForm));

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
        return "issuer_response";
    }


    private IssuanceRequest createRequestTraceing(StartIssuanceForm startIssuanceForm) {
        String contentType = "Content-Type: " + MediaType.APPLICATION_JSON;
        String authorization = "Authorization: Bearer [Maskinporten-token]";
        return new IssuanceRequest(startIssuanceForm.json(), properties.getIssuanceUrl(), authorization, contentType);
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
