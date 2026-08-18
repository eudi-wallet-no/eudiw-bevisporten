package no.idporten.eudiw.issuer.api.issuance;

import com.nimbusds.jwt.JWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import no.idporten.eudiw.issuer.issuance.preauth.PreAuthorizedIssuanceRequest;
import no.idporten.eudiw.issuer.issuance.preauth.PreAuthorizedIssuanceResponse;
import no.idporten.eudiw.issuer.issuance.preauth.PreAuthorizedIssuanceService;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationContext;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = SwaggerConfiguration.API_TAG, description = SwaggerConfiguration.API_DESCRIPTION)
@RestController
public class PreAuthorizedCredentialIssuanceController {

    private final CredentialIssuerTenantService credentialIssuerTenantService;
    private final PreAuthorizedIssuanceService preAuthorizedIssuanceService;
    private final AuthorizationServerService authorizationServerService;
    private final AccessTokenValidationService accessTokenValidationService;

    public PreAuthorizedCredentialIssuanceController(CredentialIssuerTenantService credentialIssuerTenantService, PreAuthorizedIssuanceService preAuthorizedIssuanceService, AuthorizationServerService authorizationServerService, AccessTokenValidationService accessTokenValidationService) {
        this.credentialIssuerTenantService = credentialIssuerTenantService;
        this.preAuthorizedIssuanceService = preAuthorizedIssuanceService;
        this.authorizationServerService = authorizationServerService;
        this.accessTokenValidationService = accessTokenValidationService;
    }

    @Operation(
            summary = "Start credential issuance transaction",
            description = "Upload credential data for credential issuance through the pre-authorized code flow.",
            tags = {SwaggerConfiguration.API_TAG},
            security = { @SecurityRequirement(name = "Maskinporten") }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Credential data received and ready to be issued", content = @Content(schema = @Schema(implementation = PreAuthorizedIssuanceResponse.class))),
    })
    @PostMapping(path = {Endpoints.CREDENTIAL_ISSUANCE_TRANSACTION_ENDPOINT, Endpoints.CREDENTIAL_ISSUANCE_TRANSACTION_ENDPOINT_TENANT}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PreAuthorizedIssuanceResponse> startCredentialIssuanceEndpoint(
            @Parameter(description = "Tenant identifier.", example = "bevisgenerator")
            @PathVariable(value = Endpoints.TENANT_PATH_VARIABLE, required = false) String tenant,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Start credential issuance request",
                    content = {
                            @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = PreAuthorizedIssuanceRequest.class))},
                    required = true)
            @RequestBody @Valid PreAuthorizedIssuanceRequest preAuthorizedIssuanceRequest,
            HttpServletRequest request) {
        CredentialIssuerTenant credentialIssuerTenant = credentialIssuerTenantService.findTenantById(tenant);
        JWT accessToken = accessTokenValidationService.validateAccessToken(AccessTokenValidationContext.forBearerToken(request, authorizationServerService.getPreAuthorizationServers(), credentialIssuerTenant.getCredentialIssuer()));
        accessTokenValidationService.validateAccessTokenBoundToSubject(accessToken, preAuthorizedIssuanceRequest.getSubject().getIdentifier());
        PreAuthorizedIssuanceResponse preAuthorizedIssuanceResponse = preAuthorizedIssuanceService.startIssuerTransaction(credentialIssuerTenant, preAuthorizedIssuanceRequest, accessToken);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(preAuthorizedIssuanceResponse);
    }

}
