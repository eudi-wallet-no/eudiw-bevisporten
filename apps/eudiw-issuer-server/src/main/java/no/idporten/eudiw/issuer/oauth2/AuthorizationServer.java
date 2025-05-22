package no.idporten.eudiw.issuer.oauth2;

import lombok.Data;

import java.net.URI;

@Data
public class AuthorizationServer {

    private URI issuer;
    private URI jwksUri;
    private AccessTokenValidator accessTokenValidator;

}
