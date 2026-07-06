package no.idporten.eudiw.login.openid4vp;

import no.idporten.eudiw.login.openid4vp.verifier.VerifierServiceIntegration;
import no.idporten.eudiw.login.openid4vp.verifier.model.StartVerificationRequest;
import no.idporten.eudiw.login.openid4vp.verifier.model.StartVerificationResponse;
import no.idporten.eudiw.login.openid4vp.verifier.model.VerificationResultResponse;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.protocol.*;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Service creating and verifying OpenID4VP requests and responses.
 */
@Service
public class OpenID4VPService {

    public static final String VERIFIER_CLIENT_APPLICATION_ID = "idporten-login";
    private final WalletInteractionService walletInteractionService;
    private final VerifierServiceIntegration verifierServiceIntegration;
    private final OpenIDConnectIntegration openIDConnectServer;

    public OpenID4VPService(WalletInteractionService walletInteractionService, VerifierServiceIntegration verifierServiceIntegration, OpenIDConnectIntegration openIDConnectServer) {
        this.walletInteractionService = walletInteractionService;
        this.verifierServiceIntegration = verifierServiceIntegration;
        this.openIDConnectServer = openIDConnectServer;
    }

    /**
     * Creates OpenID4VP authorization request for EUDIW PID presentation.  Users verifier service.
     *
     * @param verificationHandler verification handler
     * @param walletInteractionId
     * @return OpenID4VP authorization request
     * @throws Exception
     */
    public OpenID4VPAuthorizationRequests startVerification(VerificationHandler verificationHandler, String walletInteractionId) throws Exception {
        StartVerificationRequest startVerificationRequest = new StartVerificationRequest(
                verificationHandler.createDcqlQuery(walletInteractionId),
                createRedirectUri(walletInteractionId));
        StartVerificationResponse startVerificationResponse = verifierServiceIntegration.startVerification(VERIFIER_CLIENT_APPLICATION_ID, startVerificationRequest);
        WalletInteraction walletInteraction = walletInteractionService.getWalletInteraction(walletInteractionId);
        walletInteraction.setVerifierTransactionId(startVerificationResponse.verifierTransactionId());
        walletInteractionService.updateWalletInteraction(walletInteraction);
        return new OpenID4VPAuthorizationRequests(
                startVerificationResponse.authorizationRequest(),
                startVerificationResponse.authorizationRequestQrCode()
        );
    }

    private URI createRedirectUri(String walletInteractionId) {
        return UriComponentsBuilder.fromUri(openIDConnectServer.getSDKConfiguration().getIssuer()).pathSegment("login", walletInteractionId).build().toUri();
    }

    /**
     * Fetches result of PID presentation and authorizes the user with the embedded OIDC server.
     *
     * @return OIDC authorization response for client application
     */
    public Authorization completeVerification(VerificationHandler verificationHandler, WalletInteraction walletInteraction) throws Exception {
        VerificationResultResponse verificationResultResponse = verifierServiceIntegration.retrieveVerifiedCredentials(VERIFIER_CLIENT_APPLICATION_ID, walletInteraction.getVerifierTransactionId());
        return verificationHandler.completeVerification(verificationResultResponse.credentials().get(walletInteraction.getId()).getFirst());
    }

}
