package no.idporten.eudiw.byob.service.api;

import jakarta.validation.Valid;
import no.idporten.eudiw.byob.service.model.ByobInput;
import no.idporten.eudiw.byob.service.model.ResponseTopObject;
import no.idporten.eudiw.byob.service.serviceClasses.CredentialConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class CredentialConfigurationController {

    CredentialConfigurationService service;

    @Autowired
    public CredentialConfigurationController(CredentialConfigurationService service) {
        this.service = service;

    }


    @PostMapping(path = "/v1/credential-configurations", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, ByobInput>> createCredentialConfiguration(@Valid @RequestBody ByobInput proof){
        return ResponseEntity.ok(service.getResponseModel(proof));
    }

    @GetMapping(value = "/v1/credential-configurations", produces =  MediaType.APPLICATION_JSON_VALUE)
    public  ResponseEntity<ResponseTopObject> retrieveAllCredentialConfigurations() {
        return ResponseEntity.ok(service.getAllEntries());
    }

    @GetMapping(value = "/v1/credential-configurations/{id}", produces =  MediaType.APPLICATION_JSON_VALUE)
    public  ResponseEntity<ByobInput> retrieveSelectedCredentialConfiguration(@PathVariable String id) {
        return ResponseEntity.ofNullable(service.searchCredentialConfiguration(id));
    }
}
