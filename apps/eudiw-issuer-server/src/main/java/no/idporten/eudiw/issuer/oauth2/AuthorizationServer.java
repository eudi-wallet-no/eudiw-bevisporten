package no.idporten.eudiw.issuer.oauth2;

import lombok.Data;

import java.net.URI;

@Data
public class AuthorizationServer {

    /**
     * OAuth2 authorization server issuer.
     */
    private URI issuer;
    /**
     * OAuth2 authorization server jwks uri.
     */
    private URI jwksUri;

    /**
     * OAuth2 authorization server api key for internal api.  Used in pre authorized flow.
     */
    private String apiKey;

    /**
     * Validator for access_tokens from issuer.
     */
    private AccessTokenValidator accessTokenValidator;

}
