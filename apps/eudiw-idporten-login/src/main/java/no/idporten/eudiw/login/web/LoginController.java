package no.idporten.eudiw.login.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import no.idporten.eudiw.login.AcrValue;
import no.idporten.eudiw.login.openid4vp.*;
import no.idporten.eudiw.login.openid4vp.verifier.model.VerificationStatus;
import no.idporten.eudiw.login.openid4vp.wallet.WalletInteraction;
import no.idporten.eudiw.login.openid4vp.wallet.WalletInteractionService;
import no.idporten.sdk.oidcserver.OAuth2Exception;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * Handle browser interaction: OIDC front channel, UI with OpenID4VP authorization requests, polling.
 */
@Controller
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    private static final String SESSION_ATTRIBUTE_PUSHED_AUTHORIZATION_REQUEST = PushedAuthorizationRequest.class.getName();
    private static final String SESSION_ATTRIBUTE_WALLET_INTERACTION_ID = "WALLET_INTERACTION_ID";

    private final OpenIDConnectIntegration openIDConnectServer;
    private final OpenID4VPVerificationService openId4VpVerificationService;
    private final WalletInteractionService walletInteractionService;

    public LoginController(OpenIDConnectIntegration openIDConnectServer, OpenID4VPVerificationService openId4VpVerificationService, WalletInteractionService walletInteractionService) {
        this.openIDConnectServer = openIDConnectServer;
        this.openId4VpVerificationService = openId4VpVerificationService;
        this.walletInteractionService = walletInteractionService;
    }

    /**
     * Handle OIDC authorization request and redirect to login page.
     */
    @GetMapping("/authorize")
    public String authorize(@RequestHeader MultiValueMap<String, String> headers, @RequestParam MultiValueMap<String, String> parameters, HttpServletRequest request, HttpSession session) {
        PushedAuthorizationRequest pushedAuthorizationRequest = openIDConnectServer.process(new no.idporten.sdk.oidcserver.protocol.AuthorizationRequest(headers, parameters));
        session.setAttribute(SESSION_ATTRIBUTE_PUSHED_AUTHORIZATION_REQUEST, pushedAuthorizationRequest);
        WalletInteraction walletInteraction = walletInteractionService.createWalletInteraction();
        session.setAttribute(SESSION_ATTRIBUTE_WALLET_INTERACTION_ID, walletInteraction.getId());
        return "redirect:/login/" + walletInteraction.getId();
    }

    /**
     * Verify that wallet interaction id is stored in session.
     */
    private void verifyWalletInteraction(String walletInteractionId, HttpSession session) {
        if (! Objects.equals(walletInteractionId, session.getAttribute(SESSION_ATTRIBUTE_WALLET_INTERACTION_ID))) {
            throw new InvalidVerificationException("Invalid session");
        }
    }

    /**
     * Render login page, start polling and wait for wallet interaction, handle verification result.
     */
    @GetMapping(path = "/login/{walletInteractionId}", produces = MediaType.TEXT_HTML_VALUE)
    public String login(@PathVariable String walletInteractionId, Model model, HttpSession session) throws Exception {
        verifyWalletInteraction(walletInteractionId, session);
        final PushedAuthorizationRequest pushedAuthorizationRequest = (PushedAuthorizationRequest) session.getAttribute(SESSION_ATTRIBUTE_PUSHED_AUTHORIZATION_REQUEST);
        if (pushedAuthorizationRequest == null) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid application session", HttpStatus.BAD_REQUEST.value());
        }
        final WalletInteraction walletInteraction = walletInteractionService.retrieveWalletInteraction(walletInteractionId);
        if (walletInteraction == null) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid wallet interaction", HttpStatus.BAD_REQUEST.value());
        }
        if (! walletInteraction.isStarted()) {
            OpenID4VPAuthorizationRequests authorizationRequests = openId4VpVerificationService.startVerification(OpenID4VPVerificationHandler.forAcrValue(AcrValue.fromValue(pushedAuthorizationRequest.getResolvedAcrValue())), walletInteraction);
            model.addAttribute("authorizationRequests", authorizationRequests);
            model.addAttribute("walletInteractionId", walletInteractionId);
            model.addAttribute("pollingInterval", walletInteractionService.getWalletInteractionProperties().pollingInterval().toMillis());
            model.addAttribute("pollingTimeout", walletInteractionService.getWalletInteractionProperties().pollingTimeout().toMillis());
            walletInteractionService.updateWalletInteraction(walletInteraction);
            return "login";
        } else {
            VerificationStatus verificationStatus = openId4VpVerificationService.retrieveVerificationStatus(walletInteraction);
            if (verificationStatus == VerificationStatus.ERROR) {
                logger.warn("Verification failed for verifier transaction {}", walletInteraction.getVerifierTransactionId());
                String redirect = authorizationErrorResponse(
                        pushedAuthorizationRequest,
                        "Verification with EU Digital Identity Wallet failed");
                walletInteractionService.removeWalletInteraction(walletInteraction.getId());
                session.invalidate();
                return redirect;
            }
            Authorization authorization = openId4VpVerificationService.completeVerification(OpenID4VPVerificationHandler.forAcrValue(AcrValue.fromValue(pushedAuthorizationRequest.getResolvedAcrValue())), walletInteraction);
            AuthorizationResponse authorizationResponse = openIDConnectServer.authorize(pushedAuthorizationRequest, authorization);
            RedirectedResponse response = (RedirectedResponse) openIDConnectServer.createClientResponse(authorizationResponse);
            walletInteractionService.removeWalletInteraction(walletInteraction.getId());
            session.invalidate();
            return "redirect:" + response.toQueryRedirectUri();
        }
    }

    /**
     * Return to calling OIDC client application if user cancels login.
     */
    @GetMapping(path = "/cancel", produces = MediaType.TEXT_HTML_VALUE)
    public String cancel(HttpSession session) {
        PushedAuthorizationRequest pushedAuthorizationRequest = (PushedAuthorizationRequest) session.getAttribute(SESSION_ATTRIBUTE_PUSHED_AUTHORIZATION_REQUEST);
        session.invalidate();
        if (pushedAuthorizationRequest == null) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid application session", HttpStatus.BAD_REQUEST.value());
        }
        return authorizationErrorResponse(
                pushedAuthorizationRequest,
                "User cancelled authentication with EU Digital Identity Wallet");
    }

    private String authorizationErrorResponse(
            PushedAuthorizationRequest pushedAuthorizationRequest,
            String errorDescription) {
        AuthorizationResponse errorResponse =
                openIDConnectServer.errorResponse(pushedAuthorizationRequest, "access_denied", errorDescription);
        RedirectedResponse response =
                (RedirectedResponse) openIDConnectServer.createClientResponse(errorResponse);
        return "redirect:" + response.toQueryRedirectUri();
    }

    /**
     * Poll for wallet interaction status.  Return 202 if waiting, 200 if complete, 404 if unknown.
     */
    @GetMapping(path = "/login/{walletInteractionId}/status", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<?> poll(@PathVariable String walletInteractionId, HttpSession session) throws Exception {
        verifyWalletInteraction(walletInteractionId, session);
        WalletInteraction walletInteraction = walletInteractionService.retrieveWalletInteraction(walletInteractionId);
        if (walletInteraction == null) {
            return ResponseEntity.notFound().build();
        }
        if (! walletInteraction.isStarted()) {
            return ResponseEntity.accepted().build();
        }
        VerificationStatus verificationStatus =
                openId4VpVerificationService.retrieveVerificationStatus(walletInteraction);
        if (verificationStatus.isTerminal()) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.accepted().build();
    }

    @ExceptionHandler(InvalidVerificationException.class)
    public String handleInvalidVerificationException(InvalidVerificationException exception, HttpSession session) {
        logger.warn(exception.getLogMessage(), exception);
        return cancel(session);
    }

}
