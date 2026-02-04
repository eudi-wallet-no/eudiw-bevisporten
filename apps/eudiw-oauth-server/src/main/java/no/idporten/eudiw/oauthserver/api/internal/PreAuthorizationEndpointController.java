package no.idporten.eudiw.oauthserver.api.internal;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.oauthserver.server.OAuth2AuthorizationServer;
import no.idporten.sdk.oidcserver.protocol.PreAuthorizationRequest;
import no.idporten.sdk.oidcserver.protocol.PreAuthorizationResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Handles pre-authorization.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PreAuthorizationEndpointController {

    private final OAuth2AuthorizationServer openIDConnectSdk;

    @PostMapping(value = "/api/v1/pre-authorizations",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PreAuthorizationResponse> createPreAuthorization(@RequestHeader MultiValueMap<String, String> headers,
                                                                           @RequestBody PreAuthorizationRequest preAuthorizationRequest) {
        return ResponseEntity.ok(openIDConnectSdk.process(preAuthorizationRequest, headers));
    }

}
