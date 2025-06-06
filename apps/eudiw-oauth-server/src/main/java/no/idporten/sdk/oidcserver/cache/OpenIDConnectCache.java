package no.idporten.sdk.oidcserver.cache;

import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import no.idporten.sdk.oidcserver.protocol.Authorization;
/**
 * The server needs a cache that handles OpenID Connect/Oauth2 protocol objects with a specified lifetime.
 * The cache implementation must handle cache eviction on it's own.
 */
public interface OpenIDConnectCache {

    void putAuthorizationRequest(String requestUri, PushedAuthorizationRequest authorizationRequest);
    PushedAuthorizationRequest getAuthorizationRequest(String requestUri);
    void removeAuthorizationRequest(String requestUri);

    void putAuthorization(String code, Authorization authorization);
    Authorization getAuthorization(String code);
    void removeAuthorization(String code);

    default void putAccessTokenAndAuthorization(String token, Authorization authorization) {
        throw new UnsupportedOperationException("Cache for access tokens not implemented.");
    }

    default Authorization getAuthorizationByAccessToken(String token) {
        throw new UnsupportedOperationException("Cache for access tokens not implemented.");
    }

}
