package no.idporten.eudiw.byob.service.api;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import no.idporten.eudiw.byob.service.config.ByobServiceProperties;
import no.idporten.eudiw.byob.service.model.CredentialConfiguration;
import no.idporten.eudiw.byob.service.model.CredentialConfigurations;
import no.idporten.eudiw.byob.service.model.web.CredentialConfigurationRequestResource;
import no.idporten.eudiw.byob.service.service.CredentialConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class CredentialConfigurationController {

    CredentialConfigurationService service;
    ByobServiceProperties properties;

    @Autowired
    public CredentialConfigurationController(CredentialConfigurationService service, ByobServiceProperties properties) {
        this.properties = properties;
        this.service = service;
    }

    @Operation(
            summary = "Lag en ny bevistype",
            description = "Her kan du registrere en ny bevistype for å utforske bevisutstedelse på en enkel og rask måte.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Returner bevistypen som ble laget",
                    content = @Content(mediaType = "application/json"
                    )),
            @ApiResponse(responseCode = "500", description = "Internal error",
                    content = @Content(examples= @ExampleObject(description = "Intern feil", value = ByobServiceAPISwaggerExamples.SERVER_ERROR_EXAMPLE)))
    })
    @PostMapping(path = "/v1/credential-configuration", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CredentialConfiguration> createCredentialConfiguration(@Valid @RequestBody CredentialConfigurationRequestResource credentialConfig){
        CredentialConfiguration body = service.create(credentialConfig);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @Operation(summary = "Slett bevistype fra vct",
            description = "Slett en bevistype ved å bruke vct (verifiable credential type) som identifikator.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Returner vct som ble slettet",
                    content = @Content(mediaType = "application/json"
                    )),
            @ApiResponse(responseCode = "500", description = "Internal error",
                    content = @Content(examples= @ExampleObject(description = "Intern feil", value = ByobServiceAPISwaggerExamples.SERVER_ERROR_EXAMPLE)))
    })
    @DeleteMapping(path = "/v1/credential-configuration")
    public ResponseEntity<String> deleteCredentialConfiguration(@NotEmpty @RequestParam(name = "vct") String vct){
        service.delete(vct);
        return ResponseEntity.noContent().build();
    }

    @Hidden
    @DeleteMapping(path = "/v1/credential-configuration/all")
    public ResponseEntity<String> deleteAllCredentialConfiguration(@RequestHeader("X-API-KEY") String apiKey){
        if(apiKey == null || !apiKey.equals(properties.apiKey())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        service.deleteAll();
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Hente alle bevistyper som er laget med BYOB-en (Bring Your Own Bevis)",
            description = "Hent alle dynamiske bevistyper (Bring Your Own Bevis/BYOB)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alle bevistypene fra BYOB hentes"),
            @ApiResponse(responseCode = "500", description = "Internal error",
                    content = @Content(examples= @ExampleObject(description = "Intern feil", value = ByobServiceAPISwaggerExamples.SERVER_ERROR_EXAMPLE)))
    })
    @GetMapping(value = "/v1/credential-configurations", produces =  MediaType.APPLICATION_JSON_VALUE)
    public  ResponseEntity<CredentialConfigurations> retrieveAllCredentialConfigurations() {
        return ResponseEntity.ok(service.getAllEntries());
    }

    @Operation(
            summary = "Hent en gitt bevistype ut frå vct",
            description = "")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bevistype med matchende VCT"),
            @ApiResponse(responseCode = "500", description = "Intern feil"),
            @ApiResponse(responseCode = "404", description = "Fant ingen bevis med gitt VCT",
                    content = @Content(examples= @ExampleObject(description = "Ikke funnet", value = ByobServiceAPISwaggerExamples.NOT_FOUND)))
    })
    @GetMapping(value = "/v1/credential-configuration/{vct}", produces =  MediaType.APPLICATION_JSON_VALUE)
    public  ResponseEntity<CredentialConfiguration> retrieveCredentialConfiguration(@PathVariable String vct) {
        return ResponseEntity.ofNullable(service.getCredentialConfiguration(vct));
    }

    @Operation(
            summary = "Søk etter bevistype ut frå credentialConfigurationId",
            description = "")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bevistype med matchende credentialConfigurationId"),
            @ApiResponse(responseCode = "500", description = "Intern feil"),
            @ApiResponse(responseCode = "404", description = "Fant ingen bevis med gitt credentialConfigurationId",
                    content = @Content(examples= @ExampleObject(description = "Ikke funnet", value = ByobServiceAPISwaggerExamples.NOT_FOUND)))
    })
    @GetMapping(value = "/v1/credential-configuration/search", produces =  MediaType.APPLICATION_JSON_VALUE)
    public  ResponseEntity<CredentialConfiguration> findCredentialConfigurationByCredentialConfigurationId(@RequestParam(name = "credentialConfigurationId") String credentialConfigurationId) {
        return ResponseEntity.ofNullable(service.searchCredentialConfiguration(credentialConfigurationId));
    }
}
