package no.idporten.eudiw.oauthserver.api.internal;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.oauthserver.server.OAuth2AuthorizationServer;
import no.idporten.sdk.oidcserver.OAuth2Exception;
import no.idporten.sdk.oidcserver.protocol.PreAuthorizationRequest;
import no.idporten.sdk.oidcserver.protocol.PreAuthorizationResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Objects;

/**
 * Handles pre-authorization.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PreAuthorizationEndpointController {

    public final static String X_API_KEY_HEADER = "X-API-KEY";

    private final OAuth2AuthorizationServer openIDConnectSdk;

    @PostMapping(value = "/api/v1/pre-authorizations",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PreAuthorizationResponse> createPreAuthorization(@RequestHeader HttpHeaders headers,
                                                                           @RequestBody PreAuthorizationRequest preAuthorizationRequest) {
        checkApiKey(headers);
        return ResponseEntity.ok(openIDConnectSdk.process(preAuthorizationRequest));
    }

    private void checkApiKey(HttpHeaders headers) {
        if (!headers.containsKey(X_API_KEY_HEADER)) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Missing API key header.", 401);
        }
        if (!Objects.equals(openIDConnectSdk.getSDKConfiguration().getApiKey(), headers.getFirst(X_API_KEY_HEADER))) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid API key.", 401);
        }
    }

}
