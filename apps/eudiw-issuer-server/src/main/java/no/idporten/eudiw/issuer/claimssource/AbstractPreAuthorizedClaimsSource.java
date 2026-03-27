package no.idporten.eudiw.issuer.claimssource;

import no.idporten.eudiw.issuer.claimssource.cache.ClaimsSourceCache;
import no.idporten.eudiw.issuer.credentials.types.Claim;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public abstract non-sealed class AbstractPreAuthorizedClaimsSource extends AbstractClaimsSource implements PreAuthorizedClaimsSource {

    private ClaimsSourceCache cache;

    @Autowired
    public void setClaimsSourceCache(ClaimsSourceCache claimsSourceCache) {
        this.cache = claimsSourceCache;
    }

    @Override
    public IssuanceTransactionId store(PreAuthorizedIssuanceContext issuanceContext, final Map<String, Object> claims, Duration lifetime) {
        cache.storeClaims(issuanceContext.credentialIssuerTenant(), issuanceContext.transactionId(), claims, lifetime);
        return issuanceContext.transactionId();
    }

    @Override
    public List<Claim> issueClaims(CredentialIssueContext credentialIssueContext) {
        final IssuanceTransactionId transactionId = credentialIssueContext.transactionId();
        Map<String, Object> storedClaims = cache.retrieveClaims(credentialIssueContext.credentialIssuerTenant(), transactionId);
        return convertCredentialDataToClaims(credentialIssueContext, storedClaims);
    }

}
