package no.idporten.eudiw.oauth2.server.api;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.oauth2.server.OAuth2AuthorizationServer;
import no.idporten.eudiw.oauth2.server.protocol.ChallengeRequest;
import no.idporten.eudiw.oauth2.server.protocol.ChallengeResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Handles OAuth2 Attestation-Based Client Authentication challenge requests from clients.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChallengeEndpointController {

    private final OAuth2AuthorizationServer authorizationServer;

    @PostMapping(value = "/challenge",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChallengeResponse> challenge(@RequestHeader MultiValueMap<String, String> headers) {
        return ResponseEntity.ok(authorizationServer.process(new ChallengeRequest(headers)));
    }

}
