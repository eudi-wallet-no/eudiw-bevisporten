package no.idporten.eudiw.issuer.credentials.status.cache;

import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class InMemoryCredentialStatusCache implements CredentialStatusCache {

    private final Map<String, CredentialStatusInfo> cache = new HashMap<>();

    @Override
    public void storeCredentialStatus(CredentialIssuerTenant credentialIssuerTenant, IssuanceTransactionId transactionId, CredentialStatusInfo credentialStatusInfo, Duration lifetime) {
        String cacheKey = cacheKey("in-memory-junit", credentialIssuerTenant, transactionId);
        cache.put(cacheKey, credentialStatusInfo);

    }

    @Override
    public CredentialStatusInfo retrieveCredentialStatus(CredentialIssuerTenant credentialIssuerTenant, IssuanceTransactionId transactionId) {
        String cacheKey = cacheKey("in-memory-junit", credentialIssuerTenant, transactionId);
        return cache.get(cacheKey);
    }

}
