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

    public StartVerificationResponse startVerification(@RequestBody StartVerificationRequest startVerificationRequest, ClientApplication clientApplication) throws Exception {
        String verifierTransactionId = createVerifierTransactionId();
        String requestId = openID4VPRequestService.createRequestId(verifierTransactionId);
        URI authorizationRequestSameDevice = openID4VPRequestService.createAuthorizationRequest(requestId, clientApplication, "same_device");
        URI qrCodeDataUriCrossDevice = openID4VPRequestService.createQrCodeDataURI(openID4VPRequestService.createAuthorizationRequest(requestId, clientApplication, "cross_device"));
        verificationTransactionService.initTransaction(startVerificationRequest.dcqlQuery(), startVerificationRequest.redirectUri(), verifierTransactionId, clientApplication);
        return new StartVerificationResponse(authorizationRequestSameDevice, qrCodeDataUriCrossDevice, verifierTransactionId);
    }

    private String createVerifierTransactionId() {
        return UUID.randomUUID().toString();
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
