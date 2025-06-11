package no.idporten.eudiw.oauthserver.api;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handles OAuth2 pushed authorization requests from clients.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PushedAuthorizationRequestEndpointController {

    private final OpenIDConnectIntegration openIDConnectSdk;

    @PostMapping(value = "/par",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<PushedAuthorizationResponse> par(@RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> parameters) {
        return ResponseEntity.ok(openIDConnectSdk.process(new PushedAuthorizationRequest(headers, parameters)));
    }


}
