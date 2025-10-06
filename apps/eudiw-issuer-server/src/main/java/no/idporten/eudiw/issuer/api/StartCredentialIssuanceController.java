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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.claimssource.CredentialIssuerService;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServerService;
import no.idporten.eudiw.issuer.openid4vci.protocol.StartIssuanceRequest;
import no.idporten.eudiw.issuer.openid4vci.protocol.StartIssuanceResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = SwaggerConfiguration.API_TAG, description = SwaggerConfiguration.API_DESCRIPTION)
@RequiredArgsConstructor
@RestController
public class StartCredentialIssuanceController {

    private final CredentialIssuerService credentialIssuerService;
    private final AuthorizationServerService authorizationServerService;
    private final AccessTokenValidationService accessTokenValidationService;

    @Operation(
            summary = "Start credential issuance transaction",
            description = "Upload credential data for credential issuance through the pre-authorized code flow.",
            tags = {SwaggerConfiguration.API_TAG},
            security = { @SecurityRequirement(name = "Maskinporten") }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Credential data received and ready to be issued", content = @Content(schema = @Schema(implementation = StartIssuanceResponse.class))),
    })
    @PostMapping(path = Endpoints.CREDENTIAL_ISSUANCE_TRANSACTION_ENDPOINT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StartIssuanceResponse> startCredentialIssuanceEndpoint(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Start credential issuance request",
                    content = {
                            @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = StartIssuanceRequest.class))},
                    required = true)
            @RequestBody @Valid StartIssuanceRequest startIssuanceRequest,
            @Parameter(hidden = true)
            @RequestHeader(required = false, value = HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        JWT accessToken = accessTokenValidationService.validateAccessTokenForCredentialConfiguration(authorizationHeader, authorizationServerService.getPreAuthorizationServers());
        accessTokenValidationService.validateAccessTokenBoundToSubject(accessToken, startIssuanceRequest.getSubject().getIdentifier());
        StartIssuanceResponse startIssuanceResponse = credentialIssuerService.startIssuerTransaction(startIssuanceRequest, accessToken);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(startIssuanceResponse);
    }

}
