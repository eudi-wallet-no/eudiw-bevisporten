package no.idporten.eudiw.oauth2.server.api;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.oauth2.server.OAuth2AuthorizationServer;
import no.idporten.eudiw.oauth2.server.UseAttestationChallengeOAuth2Exception;
import no.idporten.eudiw.oauth2.server.protocol.ChallengeRequest;
import no.idporten.eudiw.oauth2.server.protocol.ChallengeResponse;
import no.idporten.eudiw.oauth2.server.protocol.TokenRequest;
import no.idporten.eudiw.oauth2.server.protocol.TokenResponse;
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

    private final OAuth2AuthorizationServer authorizationServer;

    @PostMapping(value = "/token",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<TokenResponse> token(@RequestHeader MultiValueMap<String, String> headers, @RequestParam MultiValueMap<String, String> parameters) {
        TokenResponse tokenResponse = authorizationServer.process(new TokenRequest(headers, parameters));
        ChallengeResponse nextChallenge = authorizationServer.process(new ChallengeRequest(headers));

        return ResponseEntity.ok()
                .header(UseAttestationChallengeOAuth2Exception.OAUTH_CLIENT_ATTESTATION_CHALLENGE_HEADER, nextChallenge.getAttestationChallenge())
                .body(tokenResponse);
    }

}
