package no.idporten.eudiw.connector.authoritativsources.api;

import no.idporten.eudiw.connector.authoritativsources.AuthoritativeSourcesService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthoritativSourcesController {

    private final AuthoritativeSourcesService authoritativeSourcesService;

    public AuthoritativSourcesController(AuthoritativeSourcesService authoritativeSourcesService) {
        this.authoritativeSourcesService = authoritativeSourcesService;
    }

    @PostMapping(
            value = "/api/v1/{source}/credentialdata/retrieve",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public CredentialDataResponse retrieveCredentialData(@PathVariable("source") String source, @RequestBody RetrieveRequest retrieveRequest) {
        return authoritativeSourcesService.retrieveCredentialData(source, retrieveRequest.subject());
    }
}
