package no.idporten.eudiw.issuer.api;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.openid4vci.metadata.CredentialIssuerMetadata;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class CredentialIssuerMetadataEndpointController {

    private final CredentialIssuerMetadata credentialIssuerMetadata;

    @GetMapping(value = Endpoints.METADATA_ENDPOINT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CredentialIssuerMetadata> credentialIssuerMetadataEndpoint() {
        return ResponseEntity.ok().body(credentialIssuerMetadata);
    }

}
