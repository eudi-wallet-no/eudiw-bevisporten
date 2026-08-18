package no.idporten.eudiw.oauth2.server.proxy;

import com.nimbusds.oauth2.sdk.id.Issuer;

import java.net.URI;

public record OIDCIssuerProperties(Issuer issuer, URI authorizationEndpoint, URI pushedAuthorizationRequestEndpoint, URI tokenEndpoint, URI jwksUri) {
}
