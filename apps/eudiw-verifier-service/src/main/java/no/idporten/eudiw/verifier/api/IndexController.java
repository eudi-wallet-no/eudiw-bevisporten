package no.idporten.eudiw.verifier.api;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Hidden
@Controller
public class IndexController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String index() {
        return """
                <!DOCTYPE html>
                <html lang="en">
                
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Verifier service</title>
                </head>
                <body>
                  <h1>Verifier service</h1>
                  <p>En komponent i <a href="https://docs.digdir.no/docs/lommebok/lommebok_om.html">Nasjonal sandkasse for digital lommebok</a> til bruk for å verifisere bevis med OpenID4VP.</p>
                  <p>API kan testes ut med <a href="/swagger-ui/index.html">Swagger</a>.
                </body>
                </html>
                """;
    }
}
