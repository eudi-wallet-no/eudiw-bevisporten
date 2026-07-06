package no.idporten.eudiw.login.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import no.idporten.eudiw.login.AcrValue;
import no.idporten.eudiw.login.openid4vp.*;
import no.idporten.sdk.oidcserver.OAuth2Exception;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.protocol.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Handle browser interaction: OIDC front channel, UI with OpenID4VP authorization requests.
 */
@Controller
public class UIController {

    private static final String SESSION_ATTRIBUTE_PUSHED_AUTHORIZATION_REQUEST = PushedAuthorizationRequest.class.getName();

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
        String walletInteractionId = UUID.randomUUID().toString();
        walletInteractionService.removeWalletInteraction(walletInteractionId);
        return "redirect:/login/" + walletInteractionId;
    }

    /**
     * Render login page and start polling for wallet result.
     */
    @GetMapping(path = "/login/{walletInteractionId}", produces = MediaType.TEXT_HTML_VALUE)
    public String login(@PathVariable String walletInteractionId, Model model, HttpSession session) throws Exception {
        PushedAuthorizationRequest pushedAuthorizationRequest = (PushedAuthorizationRequest) session.getAttribute(SESSION_ATTRIBUTE_PUSHED_AUTHORIZATION_REQUEST);
        if (pushedAuthorizationRequest == null) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid application session", HttpStatus.BAD_REQUEST.value());
        }
        final WalletInteraction walletInteraction = walletInteractionService.getWalletInteraction(walletInteractionId);
        if (walletInteraction == null) {
            walletInteractionService.startWalletInteraction(walletInteractionId);
            OpenID4VPAuthorizationRequests authorizationRequests = openID4VPService.startVerification(VerificationHandler.forAcrValue(AcrValue.fromValue(pushedAuthorizationRequest.getResolvedAcrValue())), walletInteractionId);
            model.addAttribute("authorizationRequests", authorizationRequests);
            model.addAttribute("walletInteractionId", walletInteractionId);
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


}
