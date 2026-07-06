package no.idporten.eudiw.issuer.credentials.status;

import no.idporten.eudiw.issuer.ErrorCode;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.CredentialIssueContext;
import no.idporten.eudiw.issuer.context.CredentialRevokeContext;
import no.idporten.eudiw.issuer.credentials.status.integration.StatusEntry;
import no.idporten.eudiw.issuer.credentials.status.integration.StatusIssuerIntegration;
import no.idporten.eudiw.issuer.credentials.status.integration.UpdatedStatusEntry;
import no.idporten.eudiw.issuer.credentials.status.persistence.CredentialStatusService;
import no.idporten.eudiw.issuer.logging.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

@Service
public class StatusIssuerService {

    private static Logger log = LoggerFactory.getLogger(StatusIssuerService.class);
    protected static final String STATUS_TYPE_INVALID = "INVALID";

    private final StatusIssuerProperties statusIssuerProperties;
    private final StatusIssuerIntegration statusIssuerIntegration;
    private final CredentialStatusService credentialStatusService;
    private final AuditService auditService;

    public StatusIssuerService(StatusIssuerProperties statusIssuerProperties, StatusIssuerIntegration statusIssuerIntegration, CredentialStatusService credentialStatusService, AuditService auditService) {
        this.statusIssuerProperties = statusIssuerProperties;
        this.statusIssuerIntegration = statusIssuerIntegration;
        this.credentialStatusService = credentialStatusService;
        this.auditService = auditService;
    }

    /**
     * Checks if feature is enabled and credential issue context indicates that a status should be allocated for the credential.
     */
    public boolean isEnabled(CredentialIssueContext credentialIssueContext) {
        return statusIssuerProperties.isEnabled()
                && credentialIssueContext.credentialConfiguration().getCredentialIssuerContext().isIncludeStatus();
    }

    public List<CredentialStatus> allocateStatus(CredentialIssueContext context, int numberOfEntries) {
        var transactionId = requireNonNull(
            context.transactionId(),
            "Transaction id is required to allocate status."
        );

        List<StatusEntry> statusEntries = statusIssuerIntegration.allocateStatusEntries(numberOfEntries);
        credentialStatusService.storeCredentialStatus(
                context.credentialIssuerTenant(),
                transactionId,
                new CredentialStatusInfo(context.credentialIssuerTenant().getId(), context.credentialConfiguration().getCredentialConfigurationId(), statusEntries));
        auditService.logIssueCredentialStatus(context, statusEntries);
        return statusEntries
                .stream()
                .map(statusEntry -> CredentialStatus.create(statusEntry.idx(), statusEntry.uri()))
                .toList();
    }

    public void revokeStatus(CredentialRevokeContext context) {
        CredentialStatusInfo credentialStatusInfo = credentialStatusService.retrieveCredentialStatus(context.credentialIssuerTenant(), context.transactionId());
        final String status = STATUS_TYPE_INVALID;
        if (credentialStatusInfo != null) {
            if (!Objects.equals(context.credentialConfiguration().getCredentialConfigurationId(), credentialStatusInfo.credentialConfigurationId())) {
                throw new IssuerServerException(ErrorCode.INVALID_REQUEST, "Not allowed to revoke credential status.", "Credential configuration id in context [%s] does not match credential status info [%s].".formatted(context.credentialConfiguration().getCredentialConfigurationId(), credentialStatusInfo.credentialConfigurationId()));
            }
            statusIssuerIntegration.updateStatusEntries(
                    credentialStatusInfo
                            .statusEntries()
                            .stream()
                            .map(entry -> new UpdatedStatusEntry(entry, status))
                            .toList());
            credentialStatusService.markCredentialRevoked(context.credentialIssuerTenant(), context.transactionId());
            auditService.logRevokeCredential(context, credentialStatusInfo.statusEntries(), status);
        } else {
            log.info("No credential status info found for transaction id {}, cannot revoke status", context.transactionId());
            auditService.logRevokeCredential(context, Collections.emptyList(), status);
        }
    }
}
