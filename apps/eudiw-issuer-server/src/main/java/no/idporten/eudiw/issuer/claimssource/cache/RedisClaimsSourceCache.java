package no.idporten.eudiw.issuer.claimssource.cache;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.cache.RedisCache;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RedisClaimsSourceCache implements ClaimsSourceCache {

    @Value("${spring.application.name}")
    private String applicationName;

    private final RedisCache cache;

    @Override
    public void storeClaims(CredentialIssuerTenant credentialIssuer, IssuanceTransactionId transactionId, Map<String, Object> claims, Duration lifetime) {
        cache.put(cacheKey(applicationName, credentialIssuer, transactionId), claims, lifetime);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> retrieveClaims(CredentialIssuerTenant credentialIssuer, IssuanceTransactionId transactionId) {
        return (Map<String, Object>) cache.remove(cacheKey(applicationName, credentialIssuer, transactionId));
    }

}
