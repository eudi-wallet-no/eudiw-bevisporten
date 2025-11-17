package no.idporten.eudiw.verifier.proxy.api.verification;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.idporten.eudiw.verifier.proxy.openid4vp.VerificationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API acting as a verification proxy.  Supports start verification, poll for status, and retrieve verification result,
 */
@Tag(name = ApiDocProperties.API_TAG, description = ApiDocProperties.API_DESCRIPTION)
@RestController
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    /**
     * Start verify.  Generate authz request uri response.
     */
    @PostMapping("/v1/verify/start")
    public ResponseEntity<StartVerificationResponse> startVerification(@RequestBody StartVerificationRequest startVerificationRequest) throws Exception {
        return ResponseEntity.ok(verificationService.startVerification(startVerificationRequest));
    }

    /**
     * Retrieve verification status.  Use for polling.
     */
    @GetMapping(value = "/v1/verify/status/{verifier_transaction_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VerificationStatusResponse> retrieveStatus(
            @Parameter(description = "Verification transaction id", example = "xyz...", required = true)
            @PathVariable("verifier_transaction_id") String verifierTransactionId) {
        return ResponseEntity.ok(verificationService.verifierStatus(verifierTransactionId));
    }

    /**
     * Retrieve verification result.
     */
    @GetMapping(value = "/v1/verify/result/{verifier_transaction_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VerificationResultResponse> retrieveVerifiedCredentials(
            @Parameter(description = "Verification transaction id", example = "xyz...", required = true)
            @PathVariable("verifier_transaction_id") String verifierTransactionId) {
        return ResponseEntity.ok(verificationService.retrieveVerificationData(verifierTransactionId));
    }

}
