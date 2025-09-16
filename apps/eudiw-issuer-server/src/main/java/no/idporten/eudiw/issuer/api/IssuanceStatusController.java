package no.idporten.eudiw.issuer.api;

import com.nimbusds.jwt.JWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServerService;
import no.idporten.eudiw.issuer.openid4vci.protocol.IssuanceStatusResponse;
import no.idporten.eudiw.issuer.openid4vci.service.IssuanceStatus;
import no.idporten.eudiw.issuer.openid4vci.service.IssuanceTransactionId;
import no.idporten.eudiw.issuer.openid4vci.service.CredentialIssuanceStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = SwaggerConfiguration.API_TAG, description = SwaggerConfiguration.API_DESCRIPTION)
@RestController
public class IssuanceStatusController {

    final Logger log = LoggerFactory.getLogger(IssuanceStatusController.class);

    private final CredentialIssuanceStatusService credentialIssuanceStatusService;
    private final AuthorizationServerService authorizationServerService;
    private final AccessTokenValidationService accessTokenValidationService;

    public IssuanceStatusController(CredentialIssuanceStatusService credentialIssuanceStatusService, AuthorizationServerService authorizationServerService, AccessTokenValidationService accessTokenValidationService) {
        this.credentialIssuanceStatusService = credentialIssuanceStatusService;
        this.authorizationServerService = authorizationServerService;
        this.accessTokenValidationService = accessTokenValidationService;
    }

    @Operation(
            summary = "Retrieve credential issuance status",
            description = "Retrieve credential issuance status.",
            tags = {SwaggerConfiguration.API_TAG},
            security = { @SecurityRequirement(name = "Maskinporten") }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status for credential issuance process", content = @Content(schema = @Schema(implementation = IssuanceStatusResponse.class))),
    })
    @GetMapping(path = Endpoints.CREDENTIAL_ISSUANCE_TRANSACTION_STATUS_ENDPOINT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IssuanceStatusResponse> issuanceStatusEndpoint(
            @Parameter(description = "Issuance transaction id", example = "xyz123...")
            @PathVariable(name = "issuance_transaction_id") String issuanceTransactionId,
            @Parameter(hidden = true)
            @RequestHeader(required = false, value = HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        JWT accessToken = accessTokenValidationService.validateAccessTokenForCredentialConfiguration(authorizationHeader, authorizationServerService.getPreAuthorizationServers());
        IssuanceStatus issuanceStatus = credentialIssuanceStatusService.getIssuanceStatus(accessToken, new IssuanceTransactionId(issuanceTransactionId));
        return ResponseEntity.ok(IssuanceStatusResponse.builder()
                .issuanceTransactionId(issuanceStatus.issuanceTransactionId())
                .status(issuanceStatus.status())
                .build());
    }

}
