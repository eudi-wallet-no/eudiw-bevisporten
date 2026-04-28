package no.idporten.eudiw.issuer.ui.web;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import no.idporten.eudiw.issuer.ui.issuer.IssuerServerService;
import no.idporten.eudiw.issuer.ui.issuer.config.CredentialConfiguration;
import no.idporten.eudiw.issuer.ui.issuer.config.IssuerServerProperties;
import no.idporten.eudiw.issuer.ui.issuer.domain.CredentialOffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Controller
public class StartIssuanceController {


    private final Logger logger = LoggerFactory.getLogger(StartIssuanceController.class);

    private final IssuerServerService issuerServerService;

    private final IssuerServerProperties properties;
    
    protected final static String VIEW_ISSUANCE_RESPONSE = "issuer_response";;

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
        model.addAttribute("credentialConfigurations", properties.getCredentialConfigurations());
        return "start";
    }

    @GetMapping("/start-issuance")
    public String startIssuance(@RequestParam("credential_configuration_id") String credentialConfigurationId, Model model) {
        logger.info("startIssuance2: " + credentialConfigurationId);

        if (credentialConfigurationId == null || credentialConfigurationId.isEmpty()) {
            credentialConfigurationId = "no.digdir.eudiw.pid_mso_mdoc";
        }
        CredentialConfiguration credentialConfiguration = properties.findCredentialConfiguration(credentialConfigurationId);

        logger.info("Starting issuance for credential_configuration_id=" + credentialConfigurationId);

        URI credentialOfferRequestUri = issuerServerService.createCredentialOfferRequestUri(credentialConfiguration);
        model.addAttribute("request", createRequestTracing(credentialOfferRequestUri));

        CredentialOffer response = issuerServerService.startIssuance(credentialConfiguration, credentialOfferRequestUri);

        String uri = convertToCredentialOfferUri(response, "openid-credential-offer");
        String haipUri = convertToCredentialOfferUri(response, "haip-vci");
        String qrCode = null;
        String haipQrCode = null;
        try {
            qrCode = Base64.getEncoder().encodeToString(createQRCodeImage(uri));
            haipQrCode = Base64.getEncoder().encodeToString(createQRCodeImage(haipUri));
        } catch (IOException | WriterException e) {
            logger.error("Failed to create QRCode for uri=" + uri, e);
            model.addAttribute("error", "Generering av QR kode feila.");
        }

        Issuance issuance = new Issuance(response.toPrettyJsonString(), uri, haipUri, qrCode, haipQrCode);
        model.addAttribute("issuance", issuance);

        return VIEW_ISSUANCE_RESPONSE;
    }

    private IssuanceRequest createRequestTracing(URI uri) {
        return new IssuanceRequest(uri.toString());
    }

    private String convertToCredentialOfferUri(CredentialOffer credentialOffer, String scheme) {
        String jsonString = credentialOffer.toJsonString();
        String offerEncoded = URLEncoder.encode(jsonString, StandardCharsets.UTF_8);
        String uri = scheme + "://?credential_offer=" + offerEncoded;
        logger.info("Issuer offer: " + credentialOffer);
        logger.info("Issuer offer encoded: " + offerEncoded);
        logger.info("Offer uri: " + uri);
        return uri;
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
