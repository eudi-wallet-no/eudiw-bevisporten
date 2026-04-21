package no.idporten.eudiw.issuer.credentials.status.cache;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.cache.RedisCache;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisCredentialStatusCache implements CredentialStatusCache {

    @Value("${spring.application.name}")
    private String applicationName;

    private final RedisCache cache;

    @Override
    public void storeCredentialStatus(CredentialIssuerTenant credentialIssuerTenant, IssuanceTransactionId transactionId, CredentialStatusInfo credentialStatusInfo, Duration lifetime) {
        cache.put(cacheKey(applicationName, credentialIssuerTenant, transactionId), credentialStatusInfo, lifetime);
    }

    @Override
    public CredentialStatusInfo retrieveCredentialStatus(CredentialIssuerTenant credentialIssuerTenant, IssuanceTransactionId transactionId) {
        return (CredentialStatusInfo) cache.get(cacheKey(applicationName, credentialIssuerTenant, transactionId));
    }

}
