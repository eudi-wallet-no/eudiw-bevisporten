package no.idporten.eudiw.login.openid4vp;

import no.idporten.eudiw.login.openid4vp.verifier.VerifierServiceIntegration;
import no.idporten.eudiw.login.openid4vp.verifier.model.DcqlQuery;
import no.idporten.eudiw.login.openid4vp.verifier.model.StartVerificationRequest;
import no.idporten.eudiw.login.openid4vp.verifier.model.StartVerificationResponse;
import no.idporten.eudiw.login.openid4vp.verifier.model.VerificationResultResponse;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.protocol.*;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

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
     * @param state
     * @return OpenID4VP authorization request
     * @throws Exception
     */
    public OpenID4VPAuthorizationRequests startVerification(String state) throws Exception {
        String dcqlQueryJson = """
                {
                  "credentials" : [ {
                    "meta" : {
                      "doctype_value" : "eu.europa.ec.eudi.pid.1"
                    },
                    "format" : "mso_mdoc",
                    "claims" : [ {
                      "path" : [ "eu.europa.ec.eudi.pid.1", "personal_administrative_number" ]
                    } ],
                    "id" : "pid"
                  }
                 ]
                }""";
        StartVerificationRequest startVerificationRequest = new StartVerificationRequest(
                DcqlQuery.parse(dcqlQueryJson),
                UriComponentsBuilder.fromUri(openIDConnectServer.getSDKConfiguration().getIssuer()).pathSegment("login", state).build().toUri());
        StartVerificationResponse startVerificationResponse = verifierServiceIntegration.startVerification(VERIFIER_CLIENT_APPLICATION_ID, startVerificationRequest);
        WalletInteraction walletInteraction = walletInteractionService.getWalletInteraction(state);
        walletInteraction.setVerifierTransactionId(startVerificationResponse.verifierTransactionId());
        walletInteractionService.updateWalletInteraction(walletInteraction);
        return new OpenID4VPAuthorizationRequests(
                startVerificationResponse.authorizationRequest(),
                startVerificationResponse.authorizationRequestQrCode()
        );
    }

    /**
     * Fetches result of PID presentation and authorizes the user with the embedded OIDC server.
     *
     * @return OIDC authorization response for client application
     */
    public ClientResponse completeAuthentication(PushedAuthorizationRequest pushedAuthorizationRequest, WalletInteraction walletInteraction) throws Exception {
        VerificationResultResponse verificationResultResponse = verifierServiceIntegration.retrieveVerifiedCredentials(VERIFIER_CLIENT_APPLICATION_ID, walletInteraction.getVerifierTransactionId());
        Authorization authorization = Authorization.builder()
                .sub(verificationResultResponse.credentials().get("pid").getFirst().claims().get("personal_administrative_number").toString())
                .acr(pushedAuthorizationRequest.getResolvedAcrValue())
                .amr("EUDIW")
                .build();
        AuthorizationResponse authorizationResponse = openIDConnectServer.authorize(pushedAuthorizationRequest, authorization);
        RedirectedResponse response = (RedirectedResponse) openIDConnectServer.createClientResponse(authorizationResponse);
        walletInteractionService.removeWalletInteraction(walletInteraction.getId());
        return response;
    }

}
