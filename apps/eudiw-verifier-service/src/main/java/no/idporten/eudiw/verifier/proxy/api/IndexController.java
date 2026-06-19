package no.idporten.eudiw.verifier.proxy.api;

import io.swagger.v3.oas.annotations.Hidden;
import no.idporten.eudiw.verifier.proxy.config.VerifierServiceProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Hidden
@Controller
public class IndexController {

    private final VerifierServiceProperties verifierProxyProperties;

    public IndexController(VerifierServiceProperties verifierProxyProperties) {
        this.verifierProxyProperties = verifierProxyProperties;
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String index() {
        return """
                <!DOCTYPE html>
                <html lang="en">
                
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Verifier proxy</title>
                </head>
                <body>
                  <h1>Verifier proxy</h1>
                  <p>En komponent i <a href="https://docs.digdir.no/docs/lommebok/lommebok_om.html">Nasjonal sandkasse for digital lommebok</a> til bruk i hackaton på Brukarrådet 2025.</p>
                  <p>API kan testes ut med <a href="/swagger-ui/index.html">Swagger</a>.
                </body>
                </html>
                """;
    }
}
