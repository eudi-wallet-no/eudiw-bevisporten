package no.idporten.eudiw.issuer.credentials.status;

import no.idporten.eudiw.issuer.ErrorCode;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.CredentialIssueContext;
import no.idporten.eudiw.issuer.context.CredentialRevokeContext;
import no.idporten.eudiw.issuer.credentials.status.cache.CredentialStatusCache;
import no.idporten.eudiw.issuer.credentials.status.cache.CredentialStatusInfo;
import no.idporten.eudiw.issuer.credentials.status.integration.StatusEntry;
import no.idporten.eudiw.issuer.credentials.status.integration.StatusIssuerIntegration;
import no.idporten.eudiw.issuer.credentials.status.integration.UpdatedStatusEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Service
public class StatusIssuerService {

    private static Logger log = LoggerFactory.getLogger(StatusIssuerService.class);
    protected static final String STATUS_TYPE_INVALID = "INVALID";

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

    public void revokeStatus(CredentialRevokeContext context) {
        CredentialStatusInfo credentialStatusInfo = credentialStatusCache.retrieveCredentialStatus(context.credentialIssuerTenant(), context.transactionId());
        if (credentialStatusInfo != null) {
            if (!Objects.equals(context.credentialConfiguration().getCredentialConfigurationId(), credentialStatusInfo.credentialConfigurationId())) {
                throw new IssuerServerException(ErrorCode.INVALID_REQUEST, "Not allowed to revoke credential status.", "Credential configuration id in context [%s] does not match credential status info [%s].".formatted(context.credentialConfiguration().getCredentialConfigurationId(), credentialStatusInfo.credentialConfigurationId()));
            }
            statusIssuerIntegration.updateStatusEntries(
                    credentialStatusInfo
                            .statusEntries()
                            .stream()
                            .map(entry -> new UpdatedStatusEntry(entry, STATUS_TYPE_INVALID))
                            .toList());
        } else {
            log.info("No credential status info found for transaction id {}, cannot revoke status", context.transactionId());
        }
    }

}
