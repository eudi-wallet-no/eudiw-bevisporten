package no.idporten.eudiw.verifier.api.openid4vp;

import io.swagger.v3.oas.annotations.Hidden;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.openid4vp.OpenID4VPRequestService;
import no.idporten.eudiw.verifier.openid4vp.OpenID4VPResponseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API used by wallet to retrieve authz request and return response.
 */
@Hidden
@RestController
public class OpenID4VPController {

    private final OpenID4VPRequestService openID4VPRequestService;
    private final OpenID4VPResponseService openID4VPResponseService;

    public OpenID4VPController(OpenID4VPRequestService openID4VPRequestService, OpenID4VPResponseService openID4VPResponseService) {
        this.openID4VPRequestService = openID4VPRequestService;
        this.openID4VPResponseService = openID4VPResponseService;
    }

    /**
     * Retrieve authz request by request_uri.  The flow parameter is used to track same device or cross device flow.
     */
    @GetMapping(value = "/openid4vp/authz-request/{request_id}", produces = "application/oauth-authz-req+jwt")
    public ResponseEntity<String> retrieveRequest(
            @PathVariable("request_id") String requestId,
            @RequestParam(name = "flow", defaultValue = "same_device", required = false) String flow) throws Exception {
        return ResponseEntity.ok(openID4VPRequestService.retrieveAuthorizationRequest(requestId, flow));
    }

    /**
     * Receive wallet response and return a wallet callback.
     */
    @PostMapping(value = "/openid4vp/authz-response/{verifier_transaction_id}")
    public ResponseEntity<WalletCallback> receiveResponse(@PathVariable("verifier_transaction_id") String verifierTransactionId,
                                                          EncryptedAuthorizationResponse encryptedAuthorizationResponse) throws Exception {
        if (encryptedAuthorizationResponse.getResponse() == null) {
            throw new VerificationException("invalid_request", "Missing authorization response");
        }
        return ResponseEntity.ok(openID4VPResponseService.receiveResponse(verifierTransactionId, encryptedAuthorizationResponse));
    }

}
