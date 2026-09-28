package no.idporten.eudiw.issuer.api.openid4vci;

import io.swagger.v3.oas.annotations.Hidden;
import no.idporten.eudiw.issuer.api.Endpoints;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenantService;
import no.idporten.eudiw.issuer.openid4vci.CredentialIssuerMetadataService;
import no.idporten.eudiw.issuer.openid4vci.metadata.CredentialIssuerMetadata;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@Hidden
@RestController
public class CredentialIssuerMetadataEndpointController {

    private static final String APPLICATION_JWT_VALUE = "application/jwt";
    private static final MediaType APPLICATION_JWT = MediaType.parseMediaType(APPLICATION_JWT_VALUE);

    private final CredentialIssuerTenantService credentialIssuerTenantService;
    private final CredentialIssuerMetadataService credentialIssuerMetadataService;

    public CredentialIssuerMetadataEndpointController(
            CredentialIssuerTenantService credentialIssuerTenantService,
            CredentialIssuerMetadataService credentialIssuerMetadataService) {
        this.credentialIssuerTenantService = credentialIssuerTenantService;
        this.credentialIssuerMetadataService = credentialIssuerMetadataService;
    }

    @GetMapping(
            path = {Endpoints.METADATA_ENDPOINT, Endpoints.METADATA_ENDPOINT_TENANT},
            produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_JWT_VALUE})
    public ResponseEntity<?> credentialIssuerMetadataEndpoint(
            @PathVariable(value = Endpoints.TENANT_PATH_VARIABLE, required = false) String tenant,
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String acceptHeader) {
        CredentialIssuerTenant credentialIssuerTenant = credentialIssuerTenantService.findTenantById(tenant);
        if (prefersSignedMetadata(acceptHeader)) {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(credentialIssuerTenant.getMetadataLifetime()))
                    .contentType(APPLICATION_JWT)
                    .body(credentialIssuerMetadataService.getSignedCredentialIssuerMetadata(credentialIssuerTenant));
        }

        CredentialIssuerMetadata metadata = credentialIssuerMetadataService.getCredentialIssuerMetadata(credentialIssuerTenant);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(credentialIssuerTenant.getMetadataLifetime()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(metadata);
    }

    private boolean prefersSignedMetadata(String acceptHeader) {
        if (acceptHeader == null || acceptHeader.isBlank()) {
            return false;
        }

        List<MediaType> acceptedMediaTypes = MediaType.parseMediaTypes(acceptHeader);
        acceptedMediaTypes.sort(Comparator
                .comparingDouble(MediaType::getQualityValue)
                .reversed()
                .thenComparing(MediaType::isWildcardType)
                .thenComparing(MediaType::isWildcardSubtype));

        for (MediaType acceptedMediaType : acceptedMediaTypes) {
            if (acceptedMediaType.getQualityValue() == 0) {
                continue;
            }
            if (acceptedMediaType.isWildcardType() || acceptedMediaType.isWildcardSubtype()) {
                return false;
            }
            if (APPLICATION_JWT.isCompatibleWith(acceptedMediaType)) {
                return true;
            }
            if (MediaType.APPLICATION_JSON.isCompatibleWith(acceptedMediaType)) {
                return false;
            }
        }

        return false;
    }

}
