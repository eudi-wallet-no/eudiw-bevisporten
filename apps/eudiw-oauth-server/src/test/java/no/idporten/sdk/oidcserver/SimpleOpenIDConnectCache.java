package no.idporten.sdk.oidcserver;

import lombok.Getter;
import no.idporten.sdk.oidcserver.cache.OpenIDConnectCache;
import no.idporten.sdk.oidcserver.protocol.Authorization;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;

import java.util.HashMap;
import java.util.Map;

/**
 *  Cache implementation used for testing.
 */
@Getter
class SimpleOpenIDConnectCache implements OpenIDConnectCache {

    private Map<String, PushedAuthorizationRequest> authorizationRequestMap = new HashMap<>();
    private Map<String, Authorization> code2authorizationMap = new HashMap<>();
    private Map<String, Authorization> accessToken2authorizationMap = new HashMap<>();

    @Override
    public void putAuthorizationRequest(String requestUri, PushedAuthorizationRequest authorizationRequest) {
        authorizationRequestMap.put(requestUri, authorizationRequest);
    }

    @Override
    public PushedAuthorizationRequest getAuthorizationRequest(String requestUri) {
        return authorizationRequestMap.get(requestUri);
    }

    @Override
    public void removeAuthorizationRequest(String requestUri) {
        authorizationRequestMap.remove(requestUri);
    }

    @Override
    public void putAuthorization(String code, Authorization authorization) {
        code2authorizationMap.put(code, authorization);
    }

    @Override
    public Authorization getAuthorization(String code) {
        return code2authorizationMap.get(code);
    }

    @Override
    public void removeAuthorization(String code) {
        code2authorizationMap.remove(code);
    }

    @Override
    public void putAccessTokenAndAuthorization(String accessToken, Authorization authorization) {
        accessToken2authorizationMap.put(accessToken, authorization);
    }

    @Override
    public Authorization getAuthorizationByAccessToken(String accessToken) {
        return accessToken2authorizationMap.get(accessToken);
    }

}
