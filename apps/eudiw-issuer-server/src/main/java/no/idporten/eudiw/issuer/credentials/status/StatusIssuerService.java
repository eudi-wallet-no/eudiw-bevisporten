package no.idporten.eudiw.issuer.credentials.status;

import no.idporten.eudiw.issuer.claimssource.CredentialIssueContext;
import no.idporten.eudiw.issuer.credentials.status.cache.CredentialStatusCache;
import no.idporten.eudiw.issuer.credentials.status.cache.CredentialStatusInfo;
import no.idporten.eudiw.issuer.credentials.status.integration.StatusEntry;
import no.idporten.eudiw.issuer.credentials.status.integration.StatusIssuerIntegration;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class StatusIssuerService {

    private final StatusIssuerProperties statusIssuerProperties;
    private final StatusIssuerIntegration statusIssuerIntegration;
    private final CredentialStatusCache credentialStatusCache;

    public StatusIssuerService(StatusIssuerProperties statusIssuerProperties, StatusIssuerIntegration statusIssuerIntegration, CredentialStatusCache credentialStatusCache) {
        this.statusIssuerProperties = statusIssuerProperties;
        this.statusIssuerIntegration = statusIssuerIntegration;
        this.credentialStatusCache = credentialStatusCache;
    }

    /**
     * Checks if feature is enabled and credential issue context indicates that a status should be allocated for the credential.
     * For now, only pre-authorized code flow.
     */
    public boolean isEnabled(CredentialIssueContext credentialIssueContext) {
        return statusIssuerProperties.isEnabled() && credentialIssueContext.transactionId() != null;
    }

    public List<CredentialStatus> allocateStatus(CredentialIssueContext context, int numberOfEntries) {
        List<StatusEntry> statusEntries = statusIssuerIntegration.allocateStatusEntries(numberOfEntries);
        credentialStatusCache.storeCredentialStatus(
                context.credentialIssuerTenant(),
                context.transactionId(),
                new CredentialStatusInfo(context.credentialIssuerTenant().getId(), context.credentialConfiguration().getCredentialConfigurationId(), statusEntries),
                Duration.ofDays(context.credentialConfiguration().getCredentialIssuerContext().getValidityDays() + 1));
        return statusEntries
                .stream()
                .map(statusEntry -> CredentialStatus.create(statusEntry.idx(), statusEntry.uri()))
                .toList();
    }

}
