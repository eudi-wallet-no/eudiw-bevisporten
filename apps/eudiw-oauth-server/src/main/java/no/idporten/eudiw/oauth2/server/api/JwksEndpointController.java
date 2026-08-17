package no.idporten.eudiw.oauth2.server.api;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.oauth2.server.OAuth2AuthorizationServer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Exposing the OAuth2 server's public keys to clients.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class JwksEndpointController {

    private final OAuth2AuthorizationServer authorizationServer;

    @GetMapping(value = {"/jwk", "/jwks", "/.well-known/jwks.json"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin(origins = "*")
    public ResponseEntity<String> jwks() {
        return ResponseEntity.ok(authorizationServer.getPublicJWKSet().toString());
    }

}
