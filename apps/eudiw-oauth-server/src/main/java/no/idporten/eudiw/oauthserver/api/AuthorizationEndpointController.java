package no.idporten.eudiw.oauthserver.api;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.protocol.*;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handles OAuth2 authorization request from client application.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthorizationEndpointController {

    private final OpenIDConnectIntegration openIDConnectSdk;

    @GetMapping("/authorize")
    public String authorize(@RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> parameters, HttpServletRequest request) {
        PushedAuthorizationRequest pushedAuthorizationRequest = openIDConnectSdk.process(new AuthorizationRequest(headers, parameters));
        Authorization authorization = Authorization.builder().sub("12345678901").attribute("scope", String.join(" ", pushedAuthorizationRequest.getScope())).build();
        AuthorizationResponse authorizationResponse = openIDConnectSdk.authorize(pushedAuthorizationRequest, authorization);
        ClientResponse clientResponse = openIDConnectSdk.createClientResponse(authorizationResponse);
        return "redirect:" + ((RedirectedResponse) clientResponse).toQueryRedirectUri();
    }

}
