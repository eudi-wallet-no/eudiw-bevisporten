package no.idporten.eudiw.oauthserver.api;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.protocol.OpenIDProviderMetadataResponse;
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

    private final OpenIDConnectIntegration openIDConnectSdk;

    @GetMapping(value = "/.well-known/oauth-authorization-server", produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin(origins = "*")
    public ResponseEntity<OpenIDProviderMetadataResponse> oAuth2AuthorizationServerMetadata() {
        return ResponseEntity.ok(openIDConnectSdk.getOpenIDProviderMetadata());
    }

    @GetMapping(value = "/.well-known/openid-configuration", produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin(origins = "*")
    public ResponseEntity<OpenIDProviderMetadataResponse> openIDConnectProviderMetadata() {
        return ResponseEntity.ok(openIDConnectSdk.getOpenIDProviderMetadata());
    }

}
