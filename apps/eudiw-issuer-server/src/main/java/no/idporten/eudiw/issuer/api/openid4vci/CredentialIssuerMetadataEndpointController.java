package no.idporten.eudiw.issuer.api.openid4vci;

import io.swagger.v3.oas.annotations.Hidden;
import no.idporten.eudiw.issuer.api.Endpoints;
import no.idporten.eudiw.issuer.openid4vci.CredentialIssuerMetadataService;
import no.idporten.eudiw.issuer.openid4vci.metadata.CredentialIssuerMetadata;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
public class CredentialIssuerMetadataEndpointController {

    private final CredentialIssuerMetadataService credentialIssuerMetadataService;

    public CredentialIssuerMetadataEndpointController(CredentialIssuerMetadataService credentialIssuerMetadataService) {
        this.credentialIssuerMetadataService = credentialIssuerMetadataService;
    }

    @GetMapping(value = Endpoints.METADATA_ENDPOINT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CredentialIssuerMetadata> credentialIssuerMetadataEndpoint() {
        return ResponseEntity.ok().body(credentialIssuerMetadataService.getCredentialIssuerMetadata("root"));
    }

}
