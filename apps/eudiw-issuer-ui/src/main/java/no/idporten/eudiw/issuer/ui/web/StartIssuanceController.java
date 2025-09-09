package no.idporten.eudiw.issuer.ui.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import no.idporten.eudiw.issuer.ui.exception.IssuerUiException;
import no.idporten.eudiw.issuer.ui.issuer.IssuerServerService;
import no.idporten.eudiw.issuer.ui.issuer.config.IssuerServerProperties;
import no.idporten.eudiw.issuer.ui.issuer.domain.CredentialOffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
        return properties.getBaseUrl();
    }

    @GetMapping("/")
    public String start(Model model) {
        model.addAttribute("credential_configuration_id", "");
        return "start";
    }

    @GetMapping("/start-issuance")
    public String startIssuance2(@RequestParam("credential_configuration_id") String credentialConfigurationId, Model model) {
        logger.info("startIssuance2: "+credentialConfigurationId);

        if (credentialConfigurationId == null || credentialConfigurationId.isEmpty()) {
            credentialConfigurationId = "no.digdir.eudiw.pid_mso_mdoc";
        }
        logger.info("Starting issuance for credential_configuration_id=" + credentialConfigurationId);

        model.addAttribute("request", createRequestTraceing(credentialConfigurationId));

        CredentialOffer response = issuerServerService.startIssuance(credentialConfigurationId);

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

    private IssuanceRequest createRequestTraceing(String offerId) {
        return new IssuanceRequest(offerId, properties.getIssuanceUrl(), null);
    }

    private String convertToCredentialOfferUri(CredentialOffer credentialOffer) {
        String jsonString = toJsonString(credentialOffer);
        String offerEncoded = URLEncoder.encode(jsonString, StandardCharsets.UTF_8);
        String uri = "openid-credential-offer://?credential_offer=" + offerEncoded;
        logger.info("Issuer offer: " + credentialOffer);
        logger.info("Issuer offer encoded: " + offerEncoded);
        return uri;
    }

    private String toJsonString(CredentialOffer credentialOffer) {
        try {
            return objectMapper.writeValueAsString(credentialOffer);
        } catch (JsonProcessingException e) {
            throw new IssuerUiException("Failed to convert credentialOffer to Json string", e);
        }
    }
    private String toPrettyJsonString(CredentialOffer credentialOffer) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(credentialOffer);
        } catch (JsonProcessingException e) {
            throw new IssuerUiException("Failed to convert credentialOffer to pretty Json string", e);
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
