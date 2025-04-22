package no.idporten.eudiw.issuer.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IndexController {

    @GetMapping("/")
    public ResponseEntity<String> index() {
        return ResponseEntity.ok(
                """
                        <html>
                           <head>
                              <title>Issuer Server</title>
                           </head>
                           <body>
                              <h1>Issuer server</h1>
                           </body>
                        </html>""");
    }

}
