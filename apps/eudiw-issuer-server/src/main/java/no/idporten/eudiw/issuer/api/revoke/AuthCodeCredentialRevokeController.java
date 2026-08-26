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
import no.idporten.eudiw.issuer.issuance.authz.SubjectCredentialIssuanceTransactionEntity;
import no.idporten.eudiw.issuer.issuance.authz.SubjectCredentialTransactionDao;
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

import java.util.List;

@Tag(name = SwaggerConfiguration.API_TAG, description = SwaggerConfiguration.API_DESCRIPTION)
@RestController
public class AuthCodeCredentialRevokeController {

    private final CredentialIssuerTenantService credentialIssuerTenantService;
    private final AuthorizationServerService authorizationServerService;
    private final AccessTokenValidationService accessTokenValidationService;
    private final SubjectCredentialTransactionDao subjectCredentialTransactionDao;
    private final StatusIssuerService statusIssuerService;
    private final RevocationResultEndpointFeature revocationResultEndpointFeature;

    public AuthCodeCredentialRevokeController(
            CredentialIssuerTenantService credentialIssuerTenantService,
            AuthorizationServerService authorizationServerService,
            AccessTokenValidationService accessTokenValidationService,
            SubjectCredentialTransactionDao subjectCredentialTransactionDao,
            StatusIssuerService statusIssuerService,
            RevocationResultEndpointFeature revocationResultEndpointFeature
    ) {
        this.credentialIssuerTenantService = credentialIssuerTenantService;
        this.authorizationServerService = authorizationServerService;
        this.accessTokenValidationService = accessTokenValidationService;
        this.subjectCredentialTransactionDao = subjectCredentialTransactionDao;
        this.statusIssuerService = statusIssuerService;
        this.revocationResultEndpointFeature = revocationResultEndpointFeature;
    }

    @Operation(
            summary = "Revoke credentials by subject for authorization code flow",
            description = "Revokes all matching credentials issued for subject identifier and credential configuration in the authorization code flow.",
            tags = {SwaggerConfiguration.API_TAG},
            security = {@SecurityRequirement(name = "Maskinporten")}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Revocation request processed."),
    })
    @PutMapping(path = {Endpoints.AUTH_CODE_CREDENTIAL_REVOKE_BY_SUBJECT_ENDPOINT, Endpoints.AUTH_CODE_CREDENTIAL_REVOKE_BY_SUBJECT_ENDPOINT_TENANT}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> revokeBySubjectEndpoint(
            @Parameter(description = "Tenant identifier.", example = "bevisgenerator")
            @PathVariable(value = Endpoints.TENANT_PATH_VARIABLE, required = false) String tenant,
            @Valid @RequestBody AuthCodeCredentialRevokeRequest revokeRequest,
            HttpServletRequest request
    ) {
        revokeBySubject(tenant, revokeRequest, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Revoke credentials by subject and return outcome",
            description = "Revokes matching credentials in the authorization code flow and returns the number of issuance transactions revoked.",
            tags = {SwaggerConfiguration.API_TAG},
            security = {@SecurityRequirement(name = "Maskinporten")}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Revocation outcome.", content = @Content(schema = @Schema(implementation = RevocationResult.class))),
            @ApiResponse(responseCode = "404", description = "Revocation outcome endpoint is not enabled."),
    })
    @PutMapping(path = {Endpoints.AUTH_CODE_CREDENTIAL_REVOKE_BY_SUBJECT_V2_ENDPOINT, Endpoints.AUTH_CODE_CREDENTIAL_REVOKE_BY_SUBJECT_V2_ENDPOINT_TENANT}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RevocationResult> revokeBySubjectV2Endpoint(
            @Parameter(description = "Tenant identifier.", example = "bevisgenerator")
            @PathVariable(value = Endpoints.TENANT_PATH_VARIABLE, required = false) String tenant,
            @Valid @RequestBody AuthCodeCredentialRevokeRequest revokeRequest,
            HttpServletRequest request
    ) {
        if (!revocationResultEndpointFeature.isEnabled()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new RevocationResult(revokeBySubject(tenant, revokeRequest, request)));
    }

    /* private */

    private int revokeBySubject(
            String tenant,
            AuthCodeCredentialRevokeRequest revokeRequest,
            HttpServletRequest request
    ) {
        CredentialIssuerTenant credentialIssuerTenant = credentialIssuerTenantService.findTenantById(tenant);
        ValidatedAuthCodeRevokeRequestContext validatedAuthCodeRevokeRequestContext = validateAuthCodeRevokeRequest(
                request,
                credentialIssuerTenant,
                revokeRequest
        );

        List<SubjectCredentialIssuanceTransactionEntity> subjectCredentialIssuanceTransactions = subjectCredentialTransactionDao.findBySubjectAndType(
                revokeRequest.subject().getIdentifier(),
                revokeRequest.credentialConfigurationId(),
                credentialIssuerTenant.getId()
        );

        // TODO: avklare hvilke credential vi revokerer ved flere treff, når revokerer vi alt som matcher subject + type.
        int revokedCount = 0;
        for (SubjectCredentialIssuanceTransactionEntity subjectCredentialIssuanceTransaction : subjectCredentialIssuanceTransactions) {
            CredentialRevokeContext credentialRevokeContext = new CredentialRevokeContext(
                    validatedAuthCodeRevokeRequestContext.accessToken(),
                    credentialIssuerTenant,
                    validatedAuthCodeRevokeRequestContext.credentialConfiguration(),
                    new IssuanceTransactionId(subjectCredentialIssuanceTransaction.getIssuanceTransactionId())
            );
            revokedCount += statusIssuerService.revokeStatus(credentialRevokeContext);
        }

        return revokedCount;
    }

    private ValidatedAuthCodeRevokeRequestContext validateAuthCodeRevokeRequest(
            HttpServletRequest request,
            CredentialIssuerTenant credentialIssuerTenant,
            AuthCodeCredentialRevokeRequest revokeRequest
    ) {
        // Temporary: this auth-code revoke endpoint is currently invoked with Maskinporten service tokens,
        // so token issuer/scope validation is performed against pre-authorization server configuration.
        JWT accessToken = accessTokenValidationService.validateAccessToken(
                AccessTokenValidationContext.forBearerToken(
                        request,
                        authorizationServerService.getPreAuthorizationServers(),
                        credentialIssuerTenant.getCredentialIssuer()
                )
        );
        ExtendedCredentialConfiguration credentialConfiguration = credentialIssuerTenant.findCredentialConfiguration(
                revokeRequest.credentialConfigurationId()
        );

        validateAccessToken(revokeRequest, accessToken, credentialConfiguration);

        return new ValidatedAuthCodeRevokeRequestContext(accessToken, credentialConfiguration);
    }

    private void validateAccessToken(AuthCodeCredentialRevokeRequest revokeRequest, JWT accessToken, ExtendedCredentialConfiguration credentialConfiguration) {
        accessTokenValidationService.validateAccessTokenForCredentialConfiguration(
                accessToken,
                AccessTokenCredentialValidationContext.forPreAuthorization(
                        credentialConfiguration.getCredentialIssuerContext().getPreAuthorizationServer(),
                        credentialConfiguration.getScope()
                )
        );
        accessTokenValidationService.validateAccessTokenBoundToSubject(accessToken, revokeRequest.subject().getIdentifier());
    }

    private record ValidatedAuthCodeRevokeRequestContext(
            JWT accessToken,
            ExtendedCredentialConfiguration credentialConfiguration
    ){}
}
