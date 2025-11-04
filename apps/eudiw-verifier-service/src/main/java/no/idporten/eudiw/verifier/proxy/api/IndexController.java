package no.idporten.eudiw.verifier.proxy.api;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

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
                  <title>Verifier proxy</title>
                </head>
                <body>
                  <h1>Verifier proxy</h1>
                  <p>En komponent i <a href="https://docs.digdir.no/docs/lommebok/lommebok_om.html">Nasjonal sandkasse for digital lommebok</a> til bruk i hackaton på Brukarrådet 2025.</p>
                </body>
                </html>
                """;

    }
}
