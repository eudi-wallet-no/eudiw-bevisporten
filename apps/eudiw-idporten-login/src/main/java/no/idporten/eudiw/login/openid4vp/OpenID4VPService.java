package no.idporten.eudiw.login.openid4vp;

import no.idporten.eudiw.login.openid4vp.verifier.VerifierServiceIntegration;
import no.idporten.eudiw.login.openid4vp.verifier.model.*;
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
public class OpenID4VPService {

    private final VerifierServiceIntegration verifierServiceIntegration;
    private final OpenIDConnectIntegration openIDConnectServer;

    public OpenID4VPService(VerifierServiceIntegration verifierServiceIntegration, OpenIDConnectIntegration openIDConnectServer) {
        this.verifierServiceIntegration = verifierServiceIntegration;
        this.openIDConnectServer = openIDConnectServer;
    }

    /**
     * Creates OpenID4VP authorization request for EUDIW PID presentation.  Users verifier service.
     *
     * @param verificationHandler verification handler
     * @param walletInteraction wallet interaction
     * @return OpenID4VP authorization request
     */
    public OpenID4VPAuthorizationRequests startVerification(VerificationHandler verificationHandler, WalletInteraction walletInteraction) throws Exception {
        StartVerificationRequest startVerificationRequest = new StartVerificationRequest(
                verificationHandler.createDcqlQuery(walletInteraction.getId()),
                createRedirectUri(walletInteraction.getId()));
        StartVerificationResponse startVerificationResponse = verifierServiceIntegration.startVerification(startVerificationRequest);
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
    public Authorization completeVerification(VerificationHandler verificationHandler, WalletInteraction walletInteraction) throws Exception {
        VerificationResultResponse verificationResultResponse = verifierServiceIntegration.retrieveVerifiedCredentials(walletInteraction.getVerifierTransactionId());
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
        VerificationStatusResponse verificationStatusResponse = verifierServiceIntegration.retrieveStatus(walletInteraction.getVerifierTransactionId());
        if ("AVAILABLE".equals(verificationStatusResponse.status())) {
            return true;
        }
        return false;
    }

}
