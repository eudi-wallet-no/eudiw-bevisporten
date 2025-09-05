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

@Tag(name = StartCredentialIssuanceController.API_TAG, description = "eIDAS 2.0 NO Sandbox Credential Issuer API")
@RequiredArgsConstructor
@RestController
public class StartCredentialIssuanceController {

    public final static String API_TAG = "eudiw-issuer-api-v1";

    private final CredentialIssuerService credentialIssuerService;
    private final AuthorizationServerService authorizationServerService;
    private final AccessTokenValidationService accessTokenValidationService;

    @Operation(
            summary = "Start credential issuance",
            description = "Upload credential data for credential issuance.",
            tags = {API_TAG},
            security = { @SecurityRequirement(name = "Maskinporten") }
    )

    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Credential data received and ready to be issued", content = @Content(schema = @Schema(implementation = StartIssuanceResponse.class))),
    })
    @PostMapping(path = Endpoints.START_CREDENTIAL_ISSUANCE_ENDPOINT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StartIssuanceResponse> startIssuanceEndpoint(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Start credential issue request",
                    content = {
                            @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = StartIssuanceRequest.class))},
                    required = true)
            @RequestBody StartIssuanceRequest startIssuanceRequest,
            @Parameter(hidden = true)
            @RequestHeader(required = false, value = HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        JWT accessToken = accessTokenValidationService.validateAccessTokenForCredentialConfiguration(authorizationHeader, authorizationServerService.getPreAuthorizationServers());
        StartIssuanceResponse startIssuanceResponse = credentialIssuerService.startIssuerTransaction(startIssuanceRequest, accessToken);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(startIssuanceResponse);
    }

}
