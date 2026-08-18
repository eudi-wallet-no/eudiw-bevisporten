package no.idporten.eudiw.issuer.api.issuance;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.api.Endpoints;
import no.idporten.eudiw.issuer.api.ErrorResponse;
import no.idporten.eudiw.issuer.issuance.authz.CredentialOfferService;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialOffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.TreeSet;

@Tag(name = CredentialOfferController.API_TAG, description = "eIDAS 2.0 NO Sandbox Credential Issuer API")
@RequiredArgsConstructor
@RestController
public class CredentialOfferController {

    public final static String API_TAG = "eudiw-issuer-api-v1";
    public final static String CREDENTIAL_OFFER_EXAMPLE = """
            {
              "credential_issuer": "https://utsteder.test.eidas2sandkasse.net",
              "credential_configuration_ids": [
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

    private final CredentialOfferService credentialOfferService;

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
    @GetMapping(path = {Endpoints.CREATE_CREDENTIAL_OFFER_ENDPOINT, Endpoints.CREATE_CREDENTIAL_OFFER_ENDPOINT_TENANT}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CredentialOffer> createCredentialOfferEndpoint(
            @Parameter(description = "Tenant identifier.", example = "bevisgenerator")
            @PathVariable(value = Endpoints.TENANT_PATH_VARIABLE, required = false) String tenant,
            @Parameter(description = "Credential configuration identifier. See credential issuer metadata.", example = "some.known.credential_mso_mdoc")
            @RequestParam(name = "credential_configuration_id", required = false) String credentialConfigurationId) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(credentialOfferService.createCredentialOffer(tenant, credentialConfigurationId));
    }

    @CrossOrigin(origins = "*", maxAge = 3600, methods = {RequestMethod.POST, RequestMethod.OPTIONS})
    @Operation(
            summary = "Create credential offer",
            description = "Create credential offer for credential issuance through the authorization code flow.",
            tags = {API_TAG}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Credential offer for credential configuration ids",
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
    @PostMapping(path = {Endpoints.CREATE_CREDENTIAL_OFFER_ENDPOINT, Endpoints.CREATE_CREDENTIAL_OFFER_ENDPOINT_TENANT}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CredentialOffer> createCredentialOfferEndpoint(
            @Parameter(description = "Tenant identifier.", example = "bevisgenerator")
            @PathVariable(value = Endpoints.TENANT_PATH_VARIABLE, required = false) String tenant,
            @Valid @RequestBody CredentialOfferRequest credentialOfferRequest) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(credentialOfferService.createCredentialOffer(tenant, new TreeSet<>(credentialOfferRequest.credentialConfigurationIds())));
    }

}
