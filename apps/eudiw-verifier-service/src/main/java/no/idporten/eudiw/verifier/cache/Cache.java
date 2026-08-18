package no.idporten.eudiw.verifier.cache;

import java.time.Duration;

public interface Cache {

    void set(String cacheKey, Object object);

    Object get(String cacheKey);

    Object remove(String cacheKey);
}
