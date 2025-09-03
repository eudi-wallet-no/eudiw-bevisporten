package no.idporten.eudiw.issuer.ui.demo;

import no.idporten.eudiw.issuer.ui.demo.issuer.IssuerServerService;
import no.idporten.eudiw.issuer.ui.demo.issuer.domain.JsonRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
        model.addAttribute("jsonRequest",new JsonRequest(defaultJsonRequest()));
        return "start";
    }

    @PostMapping("/start-issuance")
    public String startIssuance(@ModelAttribute("jsonRequest") JsonRequest jsonRequest, Model model) {
        // TODO: validering og datamodell

        String normalizedJson = jsonRequest.json().replaceAll("\\s", "");
        logger.info(normalizedJson);
        String response = issuerServerService.startIssuance(normalizedJson);
        model.addAttribute("offer", response);
        String offerEncoded = URLEncoder.encode(response.toString(), StandardCharsets.UTF_8);
        model.addAttribute("urlSameSite", "openid-credential-offer://?credential_offer="+offerEncoded);
        logger.info("Issuer response: " + response);
        return "issuer_response";
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
