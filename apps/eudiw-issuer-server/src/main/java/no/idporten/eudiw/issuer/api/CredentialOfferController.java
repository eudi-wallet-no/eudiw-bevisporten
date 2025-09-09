package no.idporten.eudiw.issuer.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.claimssource.CredentialIssuerService;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialOffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = CredentialOfferController.API_TAG, description = "eIDAS 2.0 NO Sandbox Credential Issuer API")
@RequiredArgsConstructor
@RestController
public class CredentialOfferController {

    public final static String API_TAG = "eudiw-issuer-api-v1";
    public final static String CREDENTIAL_OFFER_EXAMPLE = """
            {
              "credential_issuer": "https://utsteder.test.eidas2sandkasse.net",
              "credential_configuration_id": [
                "some.known.credential_mso_mdoc"
              ],
              "grants": {
                "authorization_code": {}
              }
            }
            """;
    public final static String ERROR_EXAMPLE = """
            {
              "error": "unknown_credential_identifier",
              "error_description": "Unknown credential identifier."
            }""";

    private final CredentialIssuerService credentialIssuerService;




    @CrossOrigin(origins = "*", maxAge = 3600, methods = {RequestMethod.GET, RequestMethod.OPTIONS})
    @Operation(
            summary = "Create credential offer",
            description = "Create credential offer for credential issuance through the authorization code flow.",
            tags = {API_TAG}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Credential offer for credential configuration id",
                    content = @Content(
                            examples = {@ExampleObject(CREDENTIAL_OFFER_EXAMPLE)},
                            schema = @Schema(implementation = CredentialOffer.class))),
            @ApiResponse(
                    responseCode = "400",
                    description = "Error response",
                    content = @Content(
                            examples = {@ExampleObject(ERROR_EXAMPLE)},
                            schema = @Schema(implementation = ErrorResponse.class))),

    })
    @GetMapping(path = Endpoints.CREATE_CREDENTIAL_OFFER_ENDPOINT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CredentialOffer> createCredentialOfferEndpoint(
            @Parameter(description = "Credential configuration identifier. See credential issuer metadata.", example = "some.known.credential_mso_mdoc")
            @RequestParam(name = "credential_configuration_id", required = false) String credentialConfigurationId) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(credentialIssuerService.createCredentialOffer(credentialConfigurationId));
    }

}
