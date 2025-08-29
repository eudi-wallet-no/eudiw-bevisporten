package no.idporten.eudiw.issuer.ui.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class IndexController {

    @GetMapping("/")
    public String start() {
        return "index";
    }

    @PostMapping("/start-issuance")
    public String startIssuance() {
        // TODO: kall mot issuer-service sitt push endpoint
        return "issuer_response";
    }


}
