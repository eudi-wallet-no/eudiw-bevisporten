package no.idporten.eudiw.connector.authoritativsources.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthoritativSourcesController {
    @PostMapping(
            value = "/api/v1/{source}/credentialdata/retrieve",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public CredentialDataResponse retrieveCredentialData(@PathVariable("source") String source, @RequestBody RetrieveRequest retrieveRequest) {
        return new CredentialDataResponse(Map.of("source", source));
    }
}
