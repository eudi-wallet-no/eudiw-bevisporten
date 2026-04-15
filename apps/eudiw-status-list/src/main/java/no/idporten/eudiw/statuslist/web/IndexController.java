package no.idporten.eudiw.statuslist.web;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class IndexController {



    @ResponseBody
    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String index() {
        return """
                <html>
                <head><title>EUDIW Statuslist</title></head>
                <body>
                <h1>EUDIW Statuslist</h1>
                <p>Statusliste for bevis utstedt i sandkassa.</p>
                </body>
                </html>
                """;
    }

}
