package no.idporten.eudiw.oauthserver.api;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.protocol.TokenRequest;
import no.idporten.sdk.oidcserver.protocol.TokenResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handles OAuth2 token requests from clients.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class TokenEndpointController {

    private final OpenIDConnectIntegration openIDConnectSdk;

    @PostMapping(value = "/token",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<TokenResponse> token(@RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> parameters) {
        return ResponseEntity.ok(openIDConnectSdk.process(new TokenRequest(headers, parameters)));
    }

}
