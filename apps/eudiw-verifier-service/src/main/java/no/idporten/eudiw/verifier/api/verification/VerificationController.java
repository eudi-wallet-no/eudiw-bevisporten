package no.idporten.eudiw.verifier.api.verification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.idporten.eudiw.verifier.config.VerifierServiceProperties;
import no.idporten.eudiw.verifier.openid4vp.VerificationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API acting as a verification backend.  Supports start verification, poll for status, and retrieve verification result,
 */
@Tag(name = ApiDocProperties.API_TAG, description = ApiDocProperties.API_DESCRIPTION)
@RestController
@RequestMapping("/api/v1/{client_application_id}")
public class VerificationController {

    private final VerificationService verificationService;
    private final VerifierServiceProperties verifierServiceProperties;

    public VerificationController(VerificationService verificationService, VerifierServiceProperties verifierServiceProperties) {
        this.verificationService = verificationService;
        this.verifierServiceProperties = verifierServiceProperties;
    }

    /**
     * Start verify.  Generate authz request uri response.
     */
    @Operation(
            summary = "Start verification.",
            description = "Start verification by inserting the client application id, and insert dcql query.",
            tags = {ApiDocProperties.API_TAG})
    @PostMapping("/verify/start/")
    public ResponseEntity<StartVerificationResponse> startVerification(@RequestBody StartVerificationRequest startVerificationRequest,
                                                                       @PathVariable("client_application_id") String clientApplicationId,
                                                                       @Parameter(
                                                                               name = "include_validation_details",
                                                                               description = "Set to true to include verification details in the response. Defaults to false.",
                                                                               example = "false",
                                                                               schema = @Schema(type = "boolean", defaultValue = "false"))
                                                                       @RequestParam(name = "include_validation_details", defaultValue = "false") boolean includeValidationDetails) throws Exception {
        return ResponseEntity.ok(verificationService.startVerification(startVerificationRequest, verifierServiceProperties.findClientApplication(clientApplicationId), includeValidationDetails));
    }

    /**
     * Retrieve verification status.  Use for polling.
     */
    @Operation(
            summary = " Retrieve verification status.",
            description = " Retrieve verification status using the verifier transaction id.",
            tags = {ApiDocProperties.API_TAG})
    @GetMapping(value = "/verify/status/{verifier_transaction_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VerificationStatusResponse> retrieveStatus(
            @PathVariable("client_application_id") String clientApplicationId,

            @Parameter(description = "Verification transaction id", example = "xyz...", required = true)
            @PathVariable("verifier_transaction_id") String verifierTransactionId) {
        return ResponseEntity.ok(verificationService.verifierStatus(verifierTransactionId, verifierServiceProperties.findClientApplication(clientApplicationId)));
    }

    /**
     * Retrieve verification result.
     */
    @Operation(
            summary = " Retrieve verification result.",
            description = " Retrieve verification result using the verifier transaction id.",
            tags = {ApiDocProperties.API_TAG})
    @GetMapping(value = "/verify/result/{verifier_transaction_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VerificationResultResponse> retrieveVerifiedCredentials(
            @PathVariable("client_application_id") String clientApplicationId,

            @Parameter(description = "Verification transaction id", example = "xyz...", required = true)
            @PathVariable("verifier_transaction_id") String verifierTransactionId) {
        return ResponseEntity.ok(verificationService.retrieveVerificationData(verifierTransactionId, verifierServiceProperties.findClientApplication(clientApplicationId)));
    }

}
