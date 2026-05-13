package no.idporten.eudiw.oauth2.server.cache;

import no.idporten.eudiw.oauth2.server.protocol.Challenge;
import no.idporten.eudiw.oauth2.server.protocol.PushedAuthorizationRequest;
import no.idporten.eudiw.oauth2.server.protocol.Authorization;
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

    void putChallenge(Challenge challenge);
    Challenge getChallenge(String challenge);
    void removeChallenge(String challenge);

    default void putAccessTokenAndAuthorization(String token, Authorization authorization) {
        throw new UnsupportedOperationException("Cache for access tokens not implemented.");
    }

    default Authorization getAuthorizationByAccessToken(String token) {
        throw new UnsupportedOperationException("Cache for access tokens not implemented.");
    }

}
