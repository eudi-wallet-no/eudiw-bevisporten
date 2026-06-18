package no.idporten.eudiw.verifier.proxy.openid4vp;

import no.idporten.eudiw.verifier.proxy.api.verification.StartVerificationRequest;
import no.idporten.eudiw.verifier.proxy.api.verification.StartVerificationResponse;
import no.idporten.eudiw.verifier.proxy.api.verification.VerificationResultResponse;
import no.idporten.eudiw.verifier.proxy.api.verification.VerificationStatusResponse;
import no.idporten.eudiw.verifier.proxy.config.VerifierProxyProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;
import java.util.UUID;

@Service
public class VerificationService {

    private final VerifierProxyProperties verifierProxyProperties;
    private final OpenID4VPRequestService openID4VPRequestService;
    private final VerificationTransactionService verificationTransactionService;

    public VerificationService(VerifierProxyProperties verifierProxyProperties, OpenID4VPRequestService openID4VPRequestService, VerificationTransactionService verificationTransactionService) {
        this.verifierProxyProperties = verifierProxyProperties;
        this.openID4VPRequestService = openID4VPRequestService;
        this.verificationTransactionService = verificationTransactionService;
    }

    public StartVerificationResponse startVerification(@RequestBody StartVerificationRequest startVerificationRequest) throws Exception {
        String verifierTransactionId = UUID.randomUUID().toString();
        URI requestUri = openID4VPRequestService.createAuthorizationRequest(verifierTransactionId);
        verificationTransactionService.initTransaction(startVerificationRequest.dcqlQuery(), verifierTransactionId);
        return new StartVerificationResponse(requestUri, verifierTransactionId);
    }
    public VerificationStatusResponse verifierStatus(String verifierTransactionId) {
        return new VerificationStatusResponse(
                verificationTransactionService.retrieveStatus(verifierTransactionId),
                verifierTransactionId);
    }

    public VerificationResultResponse retrieveVerificationData(String verifierTransactionId) {
        VerifiedCredentials verifiedCredentials = verificationTransactionService.retrieveVerifiedCredentials(verifierTransactionId);
        return new VerificationResultResponse(verifierTransactionId, verifiedCredentials.credentials());
    }

}
