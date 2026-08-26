package no.idporten.eudiw.issuer.api.revoke;

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
public class PreAuthCredentialRevokeController {

    private final CredentialIssuerTenantService credentialIssuerTenantService;
    private final AuthorizationServerService authorizationServerService;
    private final AccessTokenValidationService accessTokenValidationService;
    private final StatusIssuerService statusIssuerService;
    private final RevocationV2RichResultFeature revocationV2RichResultFeature;

    public PreAuthCredentialRevokeController(
            CredentialIssuerTenantService credentialIssuerTenantService,
            AuthorizationServerService authorizationServerService,
            AccessTokenValidationService accessTokenValidationService,
            StatusIssuerService statusIssuerService,
            RevocationV2RichResultFeature revocationV2RichResultFeature
    ) {
        this.credentialIssuerTenantService = credentialIssuerTenantService;
        this.authorizationServerService = authorizationServerService;
        this.accessTokenValidationService = accessTokenValidationService;
        this.statusIssuerService = statusIssuerService;
        this.revocationV2RichResultFeature = revocationV2RichResultFeature;
    }

    @Operation(
            summary = "Revoke credential for pre-authorized code flow",
            description = "Revoke credential in the pre-authorized code flow.",
            tags = {SwaggerConfiguration.API_TAG},
            security = {@SecurityRequirement(name = "Maskinporten")}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Revocation request processed."),
    })
    @PutMapping(path = {Endpoints.PRE_AUTH_CREDENTIAL_ISSUANCE_TRANSACTION_REVOKE_ENDPOINT, Endpoints.PRE_AUTH_CREDENTIAL_ISSUANCE_TRANSACTION_REVOKE_ENDPOINT_TENANT}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> revokePreAuthEndpoint(
            @Parameter(description = "Tenant identifier.", example = "bevisgenerator")
            @PathVariable(value = Endpoints.TENANT_PATH_VARIABLE, required = false) String tenant,
            @Valid @RequestBody PreAuthCredentialRevokeRequest preAuthCredentialRevokeRequest,
            HttpServletRequest request) {
        revokePreAuth(tenant, preAuthCredentialRevokeRequest, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Revoke credential for pre-authorized code flow and return outcome",
            description = "Revoke credential in the pre-authorized code flow and return the number of issuance transactions revoked.",
            tags = {SwaggerConfiguration.API_TAG},
            security = {@SecurityRequirement(name = "Maskinporten")}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Revocation outcome.", content = @Content(schema = @Schema(implementation = RevocationResult.class))),
            @ApiResponse(responseCode = "404", description = "Revocation outcome endpoint is not enabled."),
    })
    @PutMapping(path = {Endpoints.PRE_AUTH_CREDENTIAL_ISSUANCE_TRANSACTION_REVOKE_V2_ENDPOINT, Endpoints.PRE_AUTH_CREDENTIAL_ISSUANCE_TRANSACTION_REVOKE_V2_ENDPOINT_TENANT}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RevocationResult> revokePreAuthV2Endpoint(
            @Parameter(description = "Tenant identifier.", example = "bevisgenerator")
            @PathVariable(value = Endpoints.TENANT_PATH_VARIABLE, required = false) String tenant,
            @Valid @RequestBody PreAuthCredentialRevokeRequest preAuthCredentialRevokeRequest,
            HttpServletRequest request) {
        if (!revocationV2RichResultFeature.isEnabled()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new RevocationResult(revokePreAuth(tenant, preAuthCredentialRevokeRequest, request)));
    }

    /* private */

    private int revokePreAuth(
            String tenant,
            PreAuthCredentialRevokeRequest preAuthCredentialRevokeRequest,
            HttpServletRequest request
    ) {
        CredentialIssuerTenant credentialIssuerTenant = credentialIssuerTenantService.findTenantById(tenant);
        ValidatedPreAuthRevokeRequestContext validatedPreAuthRevokeRequestContext = validatePreAuthRevokeRequest(
                request,
                credentialIssuerTenant,
                preAuthCredentialRevokeRequest
        );

        CredentialRevokeContext context = new CredentialRevokeContext(
                validatedPreAuthRevokeRequestContext.accessToken(),
                credentialIssuerTenant,
                validatedPreAuthRevokeRequestContext.credentialConfiguration(),
                new IssuanceTransactionId(preAuthCredentialRevokeRequest.issuanceTransactionId())
        );

        return statusIssuerService.revokeStatus(context);
    }

    private ValidatedPreAuthRevokeRequestContext validatePreAuthRevokeRequest(
            HttpServletRequest request,
            CredentialIssuerTenant credentialIssuerTenant,
            PreAuthCredentialRevokeRequest preAuthCredentialRevokeRequest
    ) {
        JWT accessToken = accessTokenValidationService.validateAccessToken(
                AccessTokenValidationContext.forBearerToken(
                        request,
                        authorizationServerService.getPreAuthorizationServers(),
                        credentialIssuerTenant.getCredentialIssuer()
                )
        );
        ExtendedCredentialConfiguration credentialConfiguration = credentialIssuerTenant.findCredentialConfiguration(
                preAuthCredentialRevokeRequest.credentialConfigurationId()
        );
        accessTokenValidationService.validateAccessTokenForCredentialConfiguration(
                accessToken,
                AccessTokenCredentialValidationContext.forPreAuthorization(
                        credentialConfiguration.getCredentialIssuerContext().getPreAuthorizationServer(),
                        credentialConfiguration.getScope()
                )
        );
        return new ValidatedPreAuthRevokeRequestContext(accessToken, credentialConfiguration);
    }

    private record ValidatedPreAuthRevokeRequestContext(
            JWT accessToken,
            ExtendedCredentialConfiguration credentialConfiguration
    ) {
    }

}
