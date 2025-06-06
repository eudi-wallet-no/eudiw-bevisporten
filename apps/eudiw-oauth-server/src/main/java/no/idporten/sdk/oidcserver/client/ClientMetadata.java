package no.idporten.sdk.oidcserver.client;

import lombok.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientMetadata {

    // core config
    private String clientId;
    private String clientSecret;
    @Singular("redirectUri")
    private List<String> redirectUris;
    @Singular("scope")
    private List<String> scopes;

    // optional config
    private String clientName;
    private String logoUri;
    /**
     * A client can have a set of features.  These are not used by the SDK itself, but can be used by the application
     * using it to provide client specific features.
     */
    @Singular
    private Map<String, Object> features = new HashMap<>();

    public void validate() {
        if (clientId == null || clientId.isEmpty()) {
            throw new IllegalArgumentException("Client must have a client id.");
        }
        if (clientSecret == null || clientSecret.isEmpty()) {
            throw new IllegalArgumentException("Client %s does not have a client secret.".formatted(clientId));
        }
        if (redirectUris == null || redirectUris.isEmpty()) {
            throw new IllegalArgumentException("Client %s does not have any redirect uris.".formatted(clientId));
        }
        for (String redirectUri : redirectUris) {
            validateRedirectUri(clientId, redirectUri);
        }
        if (scopes == null || !scopes.contains("openid")) {
            throw new IllegalArgumentException("Client %s does not support the openid scope.".formatted(clientId));
        }
        if (logoUri != null) {
            validateLogoUri(clientId, logoUri);
        }
    }


    protected void validateRedirectUri(String clientId, String redirectUri)  {
        if (redirectUri == null || redirectUri.isEmpty()) {
            throw new IllegalArgumentException("Client %s has an empty redirect_uri.".formatted(clientId));
        }
        final URI uri;
        try {
            uri = new URI(redirectUri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Client %s has an redirect_uri with invalid format.".formatted(clientId));
        }
        if (! validateUriScheme(uri)) {
            throw new IllegalArgumentException("Client %s has an redirect_uri that is not http(s).".formatted(clientId));
        }
        if ("http".equals(uri.getScheme()) && uri.getHost() != null && !uri.getHost().matches("localhost|[a-z]+|127\\.0\\.0\\.1")) {
            throw new IllegalArgumentException("Client %s has an redirect_uri that is http and not localhost or a docker uri.".formatted(clientId));
        }
        if (uri.getFragment() != null) {
            throw new IllegalArgumentException("Client %s has an redirect_uri that has a fragment.".formatted(clientId));
        }
    }

    protected void validateLogoUri(String clientId, String logoUri) {
        final URI uri;
        try {
            uri = new URI(logoUri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Client %s has logo_uri with invalid format.".formatted(clientId));
        }
        if (! validateUriScheme(uri)) {
            throw new IllegalArgumentException("Client %s has logo_uri that is not http(s).".formatted(clientId));
        }

    }

    protected boolean validateUriScheme(URI uri) {
        return "http".equals(uri.getScheme()) || "https".equals(uri.getScheme());
    }

}
