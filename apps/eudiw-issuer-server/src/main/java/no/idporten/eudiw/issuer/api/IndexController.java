package no.idporten.eudiw.issuer.api;

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
                  <title>eIDAS 2.0 NO Sandbox Credential Issuer</title>
                </head>
                <body>
                  <h1>eIDAS 2.0 NO Sandbox Credential Issuer</h1>
                  <ul>
                    <li><a href="%s">Credential Issuer Metadata</li>
                    <li><a href="%s">OpenAPI definition for extended issuer API</li>
                  </ul>
                </body>
                </html>
                """.formatted(Endpoints.METADATA_ENDPOINT, Endpoints.OPENAPI_ENDPOINT);
    }

}
