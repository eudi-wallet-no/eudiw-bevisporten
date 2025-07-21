package no.idporten.eudiw.oauthserver.cache;

import lombok.RequiredArgsConstructor;
import no.idporten.sdk.oidcserver.cache.OpenIDConnectCache;
import no.idporten.sdk.oidcserver.protocol.Authorization;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class RedisOpenIDConnectCache implements OpenIDConnectCache {

    @Value("${spring.application.name}")
    private String applicationName;

    private final RedisCache cache;

    protected String parCacheKey(String key) {
        return applicationName + ":par:" + key;
    }

    protected String authCacheKey(String key) {
        return applicationName + ":auth:" + key;
    }

    @Override
    public void putAuthorizationRequest(String requestUri, PushedAuthorizationRequest authorizationRequest) {
        cache.put(parCacheKey(requestUri), authorizationRequest, Duration.of(authorizationRequest.expiresInMillis(), ChronoUnit.MILLIS));
    }

    @Override
    public PushedAuthorizationRequest getAuthorizationRequest(String requestUri) {
        return (PushedAuthorizationRequest) cache.get(parCacheKey(requestUri));
    }

    @Override
    public void removeAuthorizationRequest(String requestUri) {
        cache.remove(parCacheKey(requestUri));
    }

    @Override
    public void putAuthorization(String code, Authorization authorization) {
        cache.put(authCacheKey(code), authorization, Duration.of(authorization.expiresInMillis(), ChronoUnit.MILLIS));
    }

    @Override
    public Authorization getAuthorization(String code) {
        return (Authorization) cache.get(authCacheKey(code));
    }

    @Override
    public void removeAuthorization(String code) {
        cache.remove(authCacheKey(code));
    }
}
