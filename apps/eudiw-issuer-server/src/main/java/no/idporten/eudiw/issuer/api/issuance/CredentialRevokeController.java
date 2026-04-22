package no.idporten.eudiw.issuer.api.issuance;

import com.nimbusds.jwt.JWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import no.idporten.eudiw.issuer.api.Endpoints;
import no.idporten.eudiw.issuer.api.SwaggerConfiguration;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenantService;
import no.idporten.eudiw.issuer.context.CredentialRevokeContext;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.credentials.status.StatusIssuerService;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.eudiw.issuer.oauth2.AccessTokenCredentialValidationContext;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationContext;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServerService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = SwaggerConfiguration.API_TAG, description = SwaggerConfiguration.API_DESCRIPTION)
@RestController
public class CredentialRevokeController {

    private final CredentialIssuerTenantService credentialIssuerTenantService;
    private final AuthorizationServerService authorizationServerService;
    private final AccessTokenValidationService accessTokenValidationService;
    private final StatusIssuerService statusIssuerService;

    public CredentialRevokeController(CredentialIssuerTenantService credentialIssuerTenantService, AuthorizationServerService authorizationServerService, AccessTokenValidationService accessTokenValidationService, StatusIssuerService statusIssuerService) {
        this.credentialIssuerTenantService = credentialIssuerTenantService;
        this.authorizationServerService = authorizationServerService;
        this.accessTokenValidationService = accessTokenValidationService;
        this.statusIssuerService = statusIssuerService;
    }

    @Operation(
            summary = "Revoke credential",
            description = "Revoke credential.",
            tags = {SwaggerConfiguration.API_TAG},
            security = {@SecurityRequirement(name = "Maskinporten")}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Credential revoked."),
    })
    @PutMapping(path = {Endpoints.CREDENTIAL_ISSUANCE_TRANSACTION_REVOKE_ENDPOINT, Endpoints.CREDENTIAL_ISSUANCE_TRANSACTION_REVOKE_ENDPOINT_TENANT}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> revokeEndpoint(
            @Parameter(description = "Tenant identifier.", example = "bevisgenerator")
            @PathVariable(value = Endpoints.TENANT_PATH_VARIABLE, required = false) String tenant,
            @Valid @RequestBody CredentialRevokeRequest credentialRevokeRequest,
            HttpServletRequest request) {
        CredentialIssuerTenant credentialIssuerTenant = credentialIssuerTenantService.findTenantById(tenant);
        JWT accessToken = accessTokenValidationService.validateAccessToken(AccessTokenValidationContext.forBearerToken(request, authorizationServerService.getPreAuthorizationServers(), credentialIssuerTenant.getCredentialIssuer()));
        ExtendedCredentialConfiguration credentialConfiguration = credentialIssuerTenant.findCredentialConfiguration(credentialRevokeRequest.credentialConfigurationId());
        accessTokenValidationService.validateAccessTokenForCredentialConfiguration(accessToken, AccessTokenCredentialValidationContext.forPreAuthorization(credentialConfiguration.getCredentialIssuerContext().getPreAuthorizationServer(), credentialConfiguration.getScope()));
        CredentialRevokeContext context = new CredentialRevokeContext(accessToken, credentialIssuerTenant, credentialConfiguration, new IssuanceTransactionId(credentialRevokeRequest.issuanceTransactionId()));
        statusIssuerService.revokeStatus(context);
        return ResponseEntity.noContent().build();
    }

}
