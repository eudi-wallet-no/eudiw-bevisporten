package no.idporten.eudiw.login.oidc.api;

import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.protocol.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

/**
 * The OIDC API for this application exposed the backend OAuth2/OIDC endpoints.
 */
@Controller
public class OIDCServerController {

    private final OpenIDConnectIntegration openIDConnectServer;

    public OIDCServerController(OpenIDConnectIntegration openIDConnectServer) {
        this.openIDConnectServer = openIDConnectServer;
    }

    @GetMapping("/")
    public String redirectIndexToMetadata() {
        return "redirect:/.well-known/openid-configuration";
    }

    @GetMapping(value = "/.well-known/openid-configuration", produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin(origins = "*")
    public ResponseEntity<OpenIDProviderMetadataResponse> openIDConnectProviderMetadata() {
        return ResponseEntity.ok(openIDConnectServer.getOpenIDProviderMetadata());
    }
    @GetMapping(value = {"/jwk", "/jwks", "/.well-known/jwks.json"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin(origins = "*")
    public ResponseEntity<String> jwks() {
        return ResponseEntity.ok(openIDConnectServer.getPublicJWKSet().toString());
    }

    @PostMapping(value = "/par",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<PushedAuthorizationResponse> par(@RequestHeader MultiValueMap<String, String> headers, @RequestParam MultiValueMap<String, String> parameters) {
        PushedAuthorizationResponse pushedAuthorizationResponse = openIDConnectServer.process(new PushedAuthorizationRequest(headers, parameters));
        return ResponseEntity.status(pushedAuthorizationResponse.getHttpStatusCode()).body(pushedAuthorizationResponse);
    }

    @PostMapping(value = "/token",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<TokenResponse> token(@RequestHeader MultiValueMap<String, String> headers, @RequestParam MultiValueMap<String, String> parameters) {
        return ResponseEntity.ok(openIDConnectServer.process(new TokenRequest(headers, parameters)));
    }

}
