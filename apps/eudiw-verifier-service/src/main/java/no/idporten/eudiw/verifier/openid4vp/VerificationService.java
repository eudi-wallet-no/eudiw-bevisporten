package no.idporten.eudiw.verifier.openid4vp;

import no.idporten.eudiw.verifier.api.verification.StartVerificationRequest;
import no.idporten.eudiw.verifier.api.verification.StartVerificationResponse;
import no.idporten.eudiw.verifier.api.verification.VerificationResultResponse;
import no.idporten.eudiw.verifier.api.verification.VerificationStatusResponse;
import no.idporten.eudiw.verifier.config.ClientApplication;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;
import java.util.UUID;

@Service
public class VerificationService {

    private final OpenID4VPRequestService openID4VPRequestService;
    private final VerificationTransactionService verificationTransactionService;

    public VerificationService(OpenID4VPRequestService openID4VPRequestService, VerificationTransactionService verificationTransactionService) {
        this.openID4VPRequestService = openID4VPRequestService;
        this.verificationTransactionService = verificationTransactionService;
    }

    public StartVerificationResponse startVerification(@RequestBody StartVerificationRequest startVerificationRequest, ClientApplication clientApplication)  {

        String verifierTransactionId = UUID.randomUUID().toString();
        URI requestUri = openID4VPRequestService.createAuthorizationRequest(verifierTransactionId, clientApplication);
        verificationTransactionService.initTransaction(startVerificationRequest.dcqlQuery(), verifierTransactionId, clientApplication);
        return new StartVerificationResponse(requestUri, verifierTransactionId);
    }
    public VerificationStatusResponse verifierStatus(String verifierTransactionId, ClientApplication clientApplication)  {
        return new VerificationStatusResponse(
                verificationTransactionService.retrieveStatus(clientApplication, verifierTransactionId),
                verifierTransactionId);
    }

    public VerificationResultResponse retrieveVerificationData(String verifierTransactionId, ClientApplication clientApplication) {
        VerifiedCredentials verifiedCredentials = verificationTransactionService.retrieveVerifiedCredentials(clientApplication, verifierTransactionId);
        return new VerificationResultResponse(verifierTransactionId, verifiedCredentials.credentials());
    }

}
