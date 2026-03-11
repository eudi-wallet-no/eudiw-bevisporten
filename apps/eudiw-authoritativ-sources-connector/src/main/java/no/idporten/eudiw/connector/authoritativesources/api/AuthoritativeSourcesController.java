package no.idporten.eudiw.connector.authoritativesources.api;

import no.idporten.eudiw.connector.authoritativesources.AuthoritativeSourcesService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthoritativeSourcesController {

    private final AuthoritativeSourcesService authoritativeSourcesService;

    public AuthoritativeSourcesController(AuthoritativeSourcesService authoritativeSourcesService) {
        this.authoritativeSourcesService = authoritativeSourcesService;
    }

    @PostMapping(
            value = "/api/v1/{source}/credentialdata/retrieve",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CredentialDataResponse> retrieveCredentialData(
            @PathVariable("source") String source,
            @RequestBody RetrieveRequest retrieveRequest
    ) {
        CredentialDataResponse responseData = authoritativeSourcesService.retrieveCredentialData(source, retrieveRequest.subject());
        return ResponseEntity.ok(responseData);
    }
}
