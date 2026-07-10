package no.idporten.eudiw.login.openid4vp;

import no.idporten.eudiw.login.openid4vp.verifier.OpenID4VPVerifierServiceApiClient;
import no.idporten.eudiw.login.openid4vp.verifier.model.*;
import no.idporten.eudiw.login.openid4vp.wallet.WalletInteraction;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.protocol.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Service creating and verifying OpenID4VP requests and responses.
 */
@Service
public class OpenID4VPVerificationService {

    private final OpenID4VPVerifierServiceApiClient openID4VPVerifierServiceApiClient;
    private final OpenIDConnectIntegration openIDConnectServer;

    public OpenID4VPVerificationService(OpenID4VPVerifierServiceApiClient openID4VPVerifierServiceApiClient, OpenIDConnectIntegration openIDConnectServer) {
        this.openID4VPVerifierServiceApiClient = openID4VPVerifierServiceApiClient;
        this.openIDConnectServer = openIDConnectServer;
    }

    /**
     * Creates OpenID4VP authorization request for EUDIW PID presentation.  Users verifier service.
     *
     * @param verificationHandler verification handler
     * @param walletInteraction wallet interaction
     * @return OpenID4VP authorization request
     */
    public OpenID4VPAuthorizationRequests startVerification(OpenID4VPVerificationHandler verificationHandler, WalletInteraction walletInteraction) throws Exception {
        StartVerificationRequest startVerificationRequest = new StartVerificationRequest(
                verificationHandler.createDcqlQuery(walletInteraction.getId()),
                createRedirectUri(walletInteraction.getId()));
        StartVerificationResponse startVerificationResponse = openID4VPVerifierServiceApiClient.startVerification(startVerificationRequest);
        walletInteraction.setVerifierTransactionId(startVerificationResponse.verifierTransactionId());
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
    public Authorization completeVerification(OpenID4VPVerificationHandler verificationHandler, WalletInteraction walletInteraction) throws Exception {
        VerificationResultResponse verificationResultResponse = openID4VPVerifierServiceApiClient.retrieveVerifiedCredentials(walletInteraction.getVerifierTransactionId());
        if (CollectionUtils.isEmpty(verificationResultResponse.credentials())) {
            throw new InvalidVerificationException("Invalid verified credentials", "No credentials shared");
        }
        List<VerifiedCredential> verifiedCredentials = verificationResultResponse.credentials().get(walletInteraction.getId());
        if (verifiedCredentials.size() != 1) {
            throw new InvalidVerificationException("Invalid verified credentials", "Recieived %d credentials".formatted(verifiedCredentials.size()));
        }
        return verificationHandler.completeVerification(verifiedCredentials.getFirst());
    }

    /**
     * Checks if data is available from the verifier.
     */
    public boolean isVerificationComplete(WalletInteraction walletInteraction) {
        if (walletInteraction.getVerifierTransactionId() == null) {
            return false;
        }
        VerificationStatusResponse verificationStatusResponse = openID4VPVerifierServiceApiClient.retrieveStatus(walletInteraction.getVerifierTransactionId());
        if ("AVAILABLE".equals(verificationStatusResponse.status())) {
            return true;
        }
        return false;
    }

}
