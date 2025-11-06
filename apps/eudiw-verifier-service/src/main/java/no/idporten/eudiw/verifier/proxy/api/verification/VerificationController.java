package no.idporten.eudiw.verifier.proxy.api.verification;

import no.idporten.eudiw.verifier.proxy.openid4vp.VerificationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API acting as a verification proxy.  Supports start verification, poll for status, and retrieve verification result,
 */
@RestController
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    /**
     * Start verify.  Generate authz request uri response.
     */
    @PostMapping("/verify/start")
    public ResponseEntity<StartVerificationResponse> startVerification(@RequestBody StartVerificationRequest startVerificationRequest) throws Exception {
        return ResponseEntity.ok(verificationService.startVerification(startVerificationRequest));
    }

    /**
     * Retrieve verification status.  Use for polling.
     */
    @GetMapping(value = "/verify/status/{verifier_transaction_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VerificationStatusResponse> retrieveStatus(@PathVariable("verifier_transaction_id") String verifierTransactionId) {
        return ResponseEntity.ok(verificationService.verifierStatus(verifierTransactionId));
    }

    /**
     * Retrieve verification result.
     */
    @GetMapping(value = "/verify/result/{verifier_transaction_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VerificationDataResponse> retrieveVerifiedCredentials(@PathVariable("verifier_transaction_id") String verifierTransactionId) {
        return ResponseEntity.ok(verificationService.retrieveVerificationData(verifierTransactionId));
    }

}
