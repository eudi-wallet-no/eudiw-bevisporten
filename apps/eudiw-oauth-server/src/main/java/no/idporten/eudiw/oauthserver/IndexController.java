package no.idporten.eudiw.oauthserver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexController {

    @GetMapping("/")
    public String index() {
        return "I will be a very nice oauth2-server in near future";
    }

}
