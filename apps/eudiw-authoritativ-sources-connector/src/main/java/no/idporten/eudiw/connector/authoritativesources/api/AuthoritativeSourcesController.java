package no.idporten.eudiw.connector.authoritativesources.api;

import jakarta.validation.Valid;
import no.idporten.eudiw.connector.authoritativesources.AuthoritativeSourcesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AuthoritativeSourcesController {

    private final AuthoritativeSourcesService authoritativeSourcesService;
    private final ApiProperties apiProperties;

    public AuthoritativeSourcesController(AuthoritativeSourcesService authoritativeSourcesService, ApiProperties apiProperties) {
        this.authoritativeSourcesService = authoritativeSourcesService;
        this.apiProperties = apiProperties;
    }

    @PostMapping(
            value = "/api/v1/{source}/credentialdata/retrieve",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CredentialDataResponse> retrieveCredentialData(
            @PathVariable("source") String source,
            @RequestBody @Valid RetrieveRequest retrieveRequest,
            @RequestHeader(value = "X-API-KEY", required = false) String apiKey
    ) {

        if (apiKey == null || !apiKey.equals(apiProperties.apiKey())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API Key");
        }

        CredentialDataResponse responseData = authoritativeSourcesService.retrieveCredentialData(source, retrieveRequest);
        return ResponseEntity.ok(responseData);
    }
}
