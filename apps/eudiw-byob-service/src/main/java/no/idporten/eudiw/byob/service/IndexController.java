package no.idporten.eudiw.byob.service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/")
class IndexController {

    @GetMapping
    public String index() {
        return "Welcome to the BYOB Service";
    }
}
