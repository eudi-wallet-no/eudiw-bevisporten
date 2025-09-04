package no.idporten.eudiw.issuer.ui.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import no.idporten.eudiw.issuer.ui.demo.exception.IssuerUiException;
import no.idporten.eudiw.issuer.ui.demo.issuer.IssuerServerService;
import no.idporten.eudiw.issuer.ui.demo.issuer.domain.IssuanceResponse;
import no.idporten.eudiw.issuer.ui.demo.issuer.domain.JsonRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Controller
public class StartIssuanceController {

    Logger logger = LoggerFactory.getLogger(StartIssuanceController.class);

    private final IssuerServerService issuerServerService;

    @Autowired
    public StartIssuanceController(IssuerServerService issuerServerService) {
        this.issuerServerService = issuerServerService;
    }

    @GetMapping("/")
    public String start(Model model) {
        model.addAttribute("jsonRequest", new JsonRequest(defaultJsonRequest()));
        return "start";
    }

    @PostMapping("/start-issuance")
    public String startIssuance(@ModelAttribute("jsonRequest") JsonRequest jsonRequest, Model model) {
        String normalizedJson = jsonRequest.json().replaceAll("\\s", ""); // TODO add validation
        logger.info(normalizedJson);

        IssuanceResponse response = issuerServerService.startIssuance(normalizedJson);

        String jsonString = toJsonString(response);

        model.addAttribute("offer", response);
        String offerEncoded = URLEncoder.encode(jsonString, StandardCharsets.UTF_8);
        String uri = "openid-credential-offer://?credential_offer=" + offerEncoded;
        model.addAttribute("urlSameSite", uri);
        logger.info("Issuer offer: " + response);
        logger.info("Issuer offer encoded: " + offerEncoded);
        try {
            String qrcode = Base64.getEncoder().encodeToString(createQRCodeImage(uri));
            model.addAttribute("qrcode", qrcode);
        } catch (IOException | WriterException e) {
            // TODO handle exceptions better
            logger.error("Failed to create QRCode for uri=" + uri, e);
            model.addAttribute("error", "Failed to create QRCode");
        }
        return "issuer_response";
    }

    private String toJsonString(IssuanceResponse response) {
        try {
            return new ObjectMapper().writeValueAsString(response.credentialOffer());
        } catch (JsonProcessingException e) {
            throw new IssuerUiException("Failed to convert response to Json string", e);
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


    private String defaultJsonRequest() {
        return """
                {
                  "credential_configuration_id": "no.skatteetaten.nnid_mso_mdoc",
                  "claims": [
                    {
                      "path": [
                        "norwegian_national_id_number"
                      ],
                      "value": "1234567890"
                    },
                    {
                      "path": [
                        "norwegian_national_id_number_status"
                      ],
                      "value": "Utstedt"
                    },
                    {
                      "path": [
                        "norwegian_national_id_number_type"
                      ],
                      "value": "JUKS"
                    }
                  ]
                }
                """;
    }

}
