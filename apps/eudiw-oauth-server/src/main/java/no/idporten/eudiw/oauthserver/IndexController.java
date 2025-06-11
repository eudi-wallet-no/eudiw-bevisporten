package no.idporten.eudiw.oauthserver;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    @GetMapping("/")
    public String redirectToOAuth2ServerMetaddata() {
        return "redirect:/.well-known/oauth-authorization-server";
    }

}
