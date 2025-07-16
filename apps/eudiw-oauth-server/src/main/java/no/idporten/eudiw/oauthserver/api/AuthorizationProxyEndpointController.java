package no.idporten.eudiw.oauthserver.api;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.oauthserver.proxy.OIDCProxyService;
import no.idporten.eudiw.oauthserver.proxy.ProtocolVerifiers;
import no.idporten.eudiw.oauthserver.server.OAuth2AuthorizationServer;
import no.idporten.sdk.oidcserver.protocol.*;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handles OAuth2 authorization request from client application, interacts with remote OIDC server, and creates authorization
 * response to client.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthorizationProxyEndpointController {

    public static final String SESSION_PUSHED_AUTHORIZATION_REQUEST = "pushedAuthorizationRequest";

    private final OAuth2AuthorizationServer oAuth2AuthorizationServer;
    private final OIDCProxyService oidcProxyService;

    /**
     * Receive client authorization request and redirect a new authorization request to OIDC server.
     */
    @GetMapping("/authorize")
    public String authorize(@RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> parameters, HttpServletRequest request, HttpSession session) {
        PushedAuthorizationRequest pushedAuthorizationRequest = oAuth2AuthorizationServer.process(new AuthorizationRequest(headers, parameters));
        session.setAttribute(SESSION_PUSHED_AUTHORIZATION_REQUEST, pushedAuthorizationRequest);
        ProtocolVerifiers protocolVerifiers = ProtocolVerifiers.forLogin();
        protocolVerifiers.toHttpSession(session);
        com.nimbusds.oauth2.sdk.AuthorizationRequest authenticationRequest = oidcProxyService.createAuthorizationRequest(pushedAuthorizationRequest, protocolVerifiers);
        return "redirect:" + authenticationRequest.toURI().toString();
    }

    /**
     * Receive OIDC server authorization response, extract info and redirect a new authorization response to client.
     */
    @GetMapping("/callback")
    public String callback(@RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> parameters, HttpServletRequest request, HttpSession session) throws Exception {
        PushedAuthorizationRequest pushedAuthorizationRequest = (PushedAuthorizationRequest) session.getAttribute(SESSION_PUSHED_AUTHORIZATION_REQUEST);
        Authorization authorization = oidcProxyService.handleAuthorizationResponse(pushedAuthorizationRequest, parameters, ProtocolVerifiers.fromSession(session));
        AuthorizationResponse authorizationResponse = oAuth2AuthorizationServer.authorize(pushedAuthorizationRequest, authorization);
        ClientResponse clientResponse = oAuth2AuthorizationServer.createClientResponse(authorizationResponse);
        return "redirect:" + ((RedirectedResponse) clientResponse).toQueryRedirectUri();
    }

}
