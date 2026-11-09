package no.idporten.eudiw.oauth2.server.api;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.oauth2.server.protocol.*;
import no.idporten.eudiw.oauth2.server.proxy.OIDCProxyProperties;
import no.idporten.eudiw.oauth2.server.proxy.OIDCProxyService;
import no.idporten.eudiw.oauth2.server.proxy.ProtocolVerifiers;
import no.idporten.eudiw.oauth2.server.OpenID4VCIAuthorizationServer;
import no.idporten.eudiw.oauth2.server.util.URIUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handles OAuth2 authorization request from client application, interacts with remote OIDC server, and creates authorization
 * response to client.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthorizationProxyEndpointController {

    public static final String SESSION_PUSHED_AUTHORIZATION_REQUEST = "pushedAuthorizationRequest";

    private final OpenID4VCIAuthorizationServer openID4VCIAuthorizationServer;
    private final OIDCProxyService oidcProxyService;
    private final OIDCProxyProperties oidcProxyProperties;

    /**
     * Receive client authorization request and redirect a new authorization request to OIDC server.
     */
    @GetMapping("/authorize")
    public String authorize(@RequestHeader MultiValueMap<String, String> headers, @RequestParam MultiValueMap<String, String> parameters, HttpServletRequest request, HttpSession session) {
        PushedAuthorizationRequest pushedAuthorizationRequest = openID4VCIAuthorizationServer.process(new AuthorizationRequest(headers, parameters));
        session.setAttribute(SESSION_PUSHED_AUTHORIZATION_REQUEST, pushedAuthorizationRequest);
        if (isHeadlessLoginEnabled(pushedAuthorizationRequest)) {
            log.info("Headless login enabled for clientId: {}", pushedAuthorizationRequest.getClientId());
            return handleHeadlessLogin(pushedAuthorizationRequest);
        }
        ProtocolVerifiers protocolVerifiers = ProtocolVerifiers.forLogin();
        protocolVerifiers.toHttpSession(session);
        com.nimbusds.oauth2.sdk.AuthorizationRequest authenticationRequest = oidcProxyService.createAuthorizationRequest(pushedAuthorizationRequest, protocolVerifiers);
        return "redirect:" + authenticationRequest.toURI().toString();
    }

    private boolean isHeadlessLoginEnabled(PushedAuthorizationRequest clientAuthorizationRequest) {
        OIDCProxyProperties.HeadlessLoginProperties headlessLogin = oidcProxyProperties.getHeadlessLogin();
        boolean enabled = headlessLogin.isEnabled();
        String requestClientId = clientAuthorizationRequest.getClientId();
        var configuredClientIds = headlessLogin.getClientIds();
        boolean clientIdAllowed = configuredClientIds != null && configuredClientIds.contains(requestClientId);
        return enabled && clientIdAllowed;
    }

    private String handleHeadlessLogin(PushedAuthorizationRequest pushedAuthorizationRequest) {
        final OIDCProxyProperties.HeadlessLoginProperties headlessLogin = oidcProxyProperties.getHeadlessLogin();
        final String personIdentifier = headlessLogin.getSyntheticPid();

        Authorization.AuthorizationBuilder builder = Authorization.builder()
                .sub(personIdentifier)
                .acr(headlessLogin.getAcr())
                .amr(headlessLogin.getAmr());

        if(pushedAuthorizationRequest.getResolvedDpopJkt() != null) {
            builder.dpopJkt(pushedAuthorizationRequest.getResolvedDpopJkt());
        }

        Authorization authorization = builder.build();
        AuthorizationResponse authorizationResponse = openID4VCIAuthorizationServer.authorize(pushedAuthorizationRequest, authorization);
        ClientResponse clientResponse = openID4VCIAuthorizationServer.createClientResponse(authorizationResponse);

        return "redirect:"+clientResponse.getRedirectUri()+"?"+ URIUtils.serializeParameters(clientResponse.getParameters());
    }

    /**
     * Receive OIDC server authorization response, extract info and redirect a new authorization response to client.
     */
    @GetMapping("/callback")
    public String callback(@RequestHeader MultiValueMap<String, String> headers, @RequestParam MultiValueMap<String, String> parameters, HttpServletRequest request, HttpSession session) throws Exception {
        PushedAuthorizationRequest pushedAuthorizationRequest = (PushedAuthorizationRequest) session.getAttribute(SESSION_PUSHED_AUTHORIZATION_REQUEST);
        Authorization authorization = oidcProxyService.handleAuthorizationResponse(pushedAuthorizationRequest, parameters, ProtocolVerifiers.fromSession(session));
        AuthorizationResponse authorizationResponse = openID4VCIAuthorizationServer.authorize(pushedAuthorizationRequest, authorization);
        ClientResponse clientResponse = openID4VCIAuthorizationServer.createClientResponse(authorizationResponse);
        return "redirect:" + ((RedirectedResponse) clientResponse).toQueryRedirectUri();
    }

}
