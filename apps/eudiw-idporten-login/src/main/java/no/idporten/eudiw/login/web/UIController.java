package no.idporten.eudiw.login.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.login.AcrValue;
import no.idporten.eudiw.login.openid4vp.*;
import no.idporten.sdk.oidcserver.OAuth2Exception;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.protocol.Authorization;
import no.idporten.sdk.oidcserver.protocol.AuthorizationResponse;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import no.idporten.sdk.oidcserver.protocol.RedirectedResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * Handle browser interaction: OIDC front channel, UI with OpenID4VP authorization requests.
 */
@Slf4j
@Controller
public class UIController {

    private static final String SESSION_ATTRIBUTE_PUSHED_AUTHORIZATION_REQUEST = PushedAuthorizationRequest.class.getName();
    private static final String SESSION_ATTRIBUTE_WALLET_INTERACTION_ID = "WALLET_INTERACTION_ID";

    private final OpenIDConnectIntegration openIDConnectServer;
    private final OpenID4VPService openID4VPService;
    private final WalletInteractionService walletInteractionService;

    public UIController(OpenIDConnectIntegration openIDConnectServer, OpenID4VPService openID4VPService, WalletInteractionService walletInteractionService) {
        this.openIDConnectServer = openIDConnectServer;
        this.openID4VPService = openID4VPService;
        this.walletInteractionService = walletInteractionService;
    }

    /**
     * Handle OIDC authorization request and redirect to login page.
     */
    @GetMapping("/authorize")
    public String authorize(@RequestHeader MultiValueMap<String, String> headers, @RequestParam MultiValueMap<String, String> parameters, HttpServletRequest request, HttpSession session) {
        PushedAuthorizationRequest pushedAuthorizationRequest = openIDConnectServer.process(new no.idporten.sdk.oidcserver.protocol.AuthorizationRequest(headers, parameters));
        session.setAttribute(SESSION_ATTRIBUTE_PUSHED_AUTHORIZATION_REQUEST, pushedAuthorizationRequest);
        WalletInteraction  walletInteraction = walletInteractionService.createWalletInteraction();
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
            OpenID4VPAuthorizationRequests authorizationRequests = openID4VPService.startVerification(VerificationHandler.forAcrValue(AcrValue.fromValue(pushedAuthorizationRequest.getResolvedAcrValue())), walletInteraction);
            model.addAttribute("authorizationRequests", authorizationRequests);
            model.addAttribute("walletInteractionId", walletInteractionId);
            walletInteractionService.updateWalletInteraction(walletInteraction);
            return "login";
        } else {
            Authorization authorization = openID4VPService.completeVerification(VerificationHandler.forAcrValue(AcrValue.fromValue(pushedAuthorizationRequest.getResolvedAcrValue())), walletInteraction);
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
        AuthorizationResponse errorResponse = openIDConnectServer.errorResponse(pushedAuthorizationRequest, "access_denied", "User cancelled authentication with EU Digital Identity Wallet");
        RedirectedResponse response = (RedirectedResponse) openIDConnectServer.createClientResponse(errorResponse);
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
        if (openID4VPService.isVerificationComplete(walletInteraction)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.accepted().build();
    }

    @ExceptionHandler(InvalidVerificationException.class)
    public String handleInvalidVerificationException(InvalidVerificationException exception, HttpSession session) {
        log.warn(exception.getLogMessage(), exception);
        return cancel(session);
    }

}
