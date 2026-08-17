package no.idporten.eudiw.byob.service.api;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import no.idporten.eudiw.byob.service.config.ByobServiceProperties;
import no.idporten.eudiw.byob.service.model.CredentialConfiguration;
import no.idporten.eudiw.byob.service.model.CredentialConfigurations;
import no.idporten.eudiw.byob.service.model.web.CredentialConfigurationRequestResource;
import no.idporten.eudiw.byob.service.service.CredentialConfigurationContext;
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
    @PostMapping(value = {"/v1/public/credential-configurations", "/v1/admin/credential-configurations"}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CredentialConfiguration> createCredentialConfiguration(@Valid @RequestBody CredentialConfigurationRequestResource credentialConfig, HttpServletRequest request) {
        CredentialConfiguration body = service.create(credentialConfig, CredentialConfigurationContext.fromRequest(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @Operation(summary = "Oppdater bevistype",
            description = "Oppdater en eksisterende bevistype ved å sende inn den oppdaterte konfigurasjonen. Feltene credentialType og credentialConfigurationId kan ikke endres.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Returner bevistypen som ble oppdatert",
                    content = @Content(mediaType = "application/json"
                    )),
            @ApiResponse(responseCode = "500", description = "Internal error",
                    content = @Content(examples= @ExampleObject(description = "Intern feil", value = ByobServiceAPISwaggerExamples.SERVER_ERROR_EXAMPLE)))
    })
    @PutMapping(value = {"/v1/public/credential-configurations", "/v1/admin/credential-configurations"})
    public ResponseEntity<CredentialConfiguration> updateCredentialConfiguration(@Valid @RequestBody CredentialConfigurationRequestResource credentialConfig, HttpServletRequest request) {
        CredentialConfiguration body = service.update(credentialConfig, CredentialConfigurationContext.fromRequest(request));
        return ResponseEntity.ok().body(body);
    }

    @Operation(summary = "Slett bevistype",
            description = "Slett en bevistype ved å bruke credential type (verifiable credential type) som identifikator.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Returner credential type som ble slettet",
                    content = @Content(mediaType = "application/json"
                    )),
            @ApiResponse(responseCode = "500", description = "Internal error",
                    content = @Content(examples= @ExampleObject(description = "Intern feil", value = ByobServiceAPISwaggerExamples.SERVER_ERROR_EXAMPLE)))
    })
    @DeleteMapping(value = {"/v1/public/credential-configurations", "/v1/admin/credential-configurations"})
    public ResponseEntity<String> deleteCredentialConfiguration(@NotEmpty @RequestParam(name = "credential-type") String credentialType, HttpServletRequest request) {
        service.delete(credentialType, CredentialConfigurationContext.fromRequest(request));
        return ResponseEntity.noContent().build();
    }

    @Hidden
    @DeleteMapping(path = "/v1/admin/credential-configurations/all")
    public ResponseEntity<String> deleteAllCredentialConfiguration(@RequestHeader("X-API-KEY") String apiKey){
        if(apiKey == null || !apiKey.equals(properties.apiKey())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        service.deleteAll();
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Hente alle bevistyper fra BYOB'en (Bring Your Own Bevis)",
            description = "Hent alle dynamiske bevistyper som kan utstedes (Bring Your Own Bevis/BYOB)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alle bevistypene fra BYOB hentes, også de som ikke kan redigeres"),
            @ApiResponse(responseCode = "500", description = "Internal error",
                    content = @Content(examples= @ExampleObject(description = "Intern feil", value = ByobServiceAPISwaggerExamples.SERVER_ERROR_EXAMPLE)))
    })
    @GetMapping(value = {"/v1/public/credential-configurations", "/v1/admin/credential-configurations"}, produces =  MediaType.APPLICATION_JSON_VALUE)
    public  ResponseEntity<CredentialConfigurations> retrieveAllCredentialConfigurationsForIssue(HttpServletRequest request) {
        return ResponseEntity.ok(service.getAllEntries(CredentialConfigurationContext.fromRequest(request)));
    }

    @Operation(
            summary = "Hent en gitt bevistype ut frå credential type (vct, doctype)",
            description = "")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bevistype med matchende credential type"),
            @ApiResponse(responseCode = "500", description = "Intern feil"),
            @ApiResponse(responseCode = "404", description = "Fant ingen bevis med gitt credential type",
                    content = @Content(examples= @ExampleObject(description = "Ikke funnet", value = ByobServiceAPISwaggerExamples.NOT_FOUND)))
    })
    @GetMapping(value = {"/v1/public/credential-configurations/{credentialType}", "/v1/admin/credential-configurations/{credentialType}"}, produces =  MediaType.APPLICATION_JSON_VALUE)
    public  ResponseEntity<CredentialConfiguration> retrieveCredentialConfiguration(@PathVariable String credentialType, HttpServletRequest request) {
        return ResponseEntity.ofNullable(service.getCredentialConfiguration(credentialType, CredentialConfigurationContext.fromRequest(request)));
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
    @GetMapping(value = {"/v1/public/credential-configurations/search", "/v1/admin/credential-configurations/search"}, produces =  MediaType.APPLICATION_JSON_VALUE)
    public  ResponseEntity<CredentialConfiguration> findCredentialConfigurationByCredentialConfigurationId(@RequestParam(name = "credentialConfigurationId") String credentialConfigurationId) {
        return ResponseEntity.ofNullable(service.searchCredentialConfiguration(credentialConfigurationId));
    }
}
