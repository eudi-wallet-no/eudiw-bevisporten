package no.idporten.eudiw.oauth2.server.api;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.oauth2.server.OAuth2AuthorizationServer;
import no.idporten.eudiw.oauth2.server.protocol.OpenIDProviderMetadataResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Exposing the OAuth2 server's metadata to clients.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class OpenIDConfigurationEndpointController {

    private final OAuth2AuthorizationServer authorizationServer;

    @GetMapping(value = "/.well-known/oauth-authorization-server", produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin(origins = "*")
    public ResponseEntity<OpenIDProviderMetadataResponse> oAuth2AuthorizationServerMetadata() {
        return ResponseEntity.ok(authorizationServer.getOpenIDProviderMetadata());
    }

    @GetMapping(value = "/.well-known/openid-configuration", produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin(origins = "*")
    public ResponseEntity<OpenIDProviderMetadataResponse> openIDConnectProviderMetadata() {
        return ResponseEntity.ok(authorizationServer.getOpenIDProviderMetadata());
    }

}
