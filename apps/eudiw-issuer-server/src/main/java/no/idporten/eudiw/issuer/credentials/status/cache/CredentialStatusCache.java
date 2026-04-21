package no.idporten.eudiw.issuer.credentials.status.cache;

import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;

import java.time.Duration;

/**
 * Data cache for credential status.
 */
public interface CredentialStatusCache {

    /**
     * Calculates a cache key.
     */
    default String cacheKey(String cachePrefix, CredentialIssuerTenant credentialIssuer, IssuanceTransactionId transactionId) {
        return cachePrefix + ":credential:status:" + credentialIssuer.getId() + ":" + transactionId.getValue();
    }

    void storeCredentialStatus(CredentialIssuerTenant credentialIssuerTenant, IssuanceTransactionId transactionId, CredentialStatusInfo credentialStatusInfo, Duration lifetime);

    CredentialStatusInfo retrieveCredentialStatus(CredentialIssuerTenant credentialIssuerTenant, IssuanceTransactionId transactionId);

}
