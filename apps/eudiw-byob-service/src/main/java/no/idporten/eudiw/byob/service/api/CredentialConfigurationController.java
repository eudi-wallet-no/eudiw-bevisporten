package no.idporten.eudiw.byob.service.api;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class CredentialConfigurationController {

    private Map<String, Object> persistenceLayer = new HashMap<>();

    @PostMapping(path = "/v1/credential-configurations", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createCredentialConfiguration(@RequestBody Map<String, Object> json) {
        String credentialConfigurationId = UUID.randomUUID().toString();
        json.put("credential_configuration_id", credentialConfigurationId);
        persistenceLayer.put(credentialConfigurationId, json);
        return ResponseEntity.ok(json);
    }

    @GetMapping(value = "/v1/credential-configurations", produces =  MediaType.APPLICATION_JSON_VALUE)
    public  ResponseEntity<CredentialConfigurationResponse> retrieveAllCredentialConfigurations() {
        return ResponseEntity.ok(new CredentialConfigurationResponse(persistenceLayer.values().stream().toList()));
    }

}
