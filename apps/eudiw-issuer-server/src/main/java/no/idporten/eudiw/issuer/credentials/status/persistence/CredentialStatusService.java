package no.idporten.eudiw.issuer.credentials.status.persistence;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.credentials.status.CredentialStatusInfo;
import no.idporten.eudiw.issuer.credentials.status.integration.StatusEntry;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CredentialStatusService {

    private final CredentialIssuanceTransactionDao issuanceTransactionDao;
    private final StatusListEntryDao statusListEntryDao;

    @Transactional
    public void storeCredentialStatus(CredentialIssuerTenant tenant, IssuanceTransactionId transactionId, CredentialStatusInfo credentialStatusInfo) {
        CredentialIssuanceTransactionEntity transaction = issuanceTransactionDao.findByIssuanceTransactionId(
                        transactionId.getValue(),
                        tenant.getId()
                )
                .orElseThrow(() -> new IllegalStateException("Missing issuance transaction for status entries"));

        statusListEntryDao.insertEntries(transaction.getId(), credentialStatusInfo.statusEntries());
        issuanceTransactionDao.updateUpdatedMs(transaction.getId(), System.currentTimeMillis());
    }

    @Transactional(readOnly = true)
    public CredentialStatusInfo retrieveCredentialStatus(CredentialIssuerTenant tenant, IssuanceTransactionId transactionId) {
        Optional<CredentialIssuanceTransactionEntity> transaction = issuanceTransactionDao.findByIssuanceTransactionId(
                transactionId.getValue(),
                tenant.getId()
        );

        if (transaction.isEmpty()) {
            return null;
        }

        CredentialIssuanceTransactionEntity transactionEntity = transaction.get();
        List<StatusListEntryEntity> statusListEntries = statusListEntryDao.findEntries(transactionEntity.getId());

        return toCredentialStatusInfo(transactionEntity, statusListEntries);
    }

    @Transactional(readOnly = true)
    public CredentialStatusInfo retrieveCredentialStatus(CredentialIssuerTenant tenant, IssuanceTransactionId transactionId, String credentialConfigurationId) {
        Optional<CredentialIssuanceTransactionEntity> transaction = issuanceTransactionDao.findTransaction(
                transactionId.getValue(),
                credentialConfigurationId,
                tenant.getId()
        );

        if (transaction.isEmpty()) {
            return null;
        }

        CredentialIssuanceTransactionEntity transactionEntity = transaction.get();
        List<StatusListEntryEntity> statusListEntries = statusListEntryDao.findEntries(transactionEntity.getId());

        return toCredentialStatusInfo(transactionEntity, statusListEntries);
    }

    @Transactional
    public void markCredentialRevoked(CredentialIssuerTenant tenant, IssuanceTransactionId transactionId) {
        long now = System.currentTimeMillis();
        issuanceTransactionDao.updateRevokedMs(transactionId.getValue(), tenant.getId(), now, now);
    }

    private CredentialStatusInfo toCredentialStatusInfo(
            CredentialIssuanceTransactionEntity transaction,
            List<StatusListEntryEntity> statusListEntries
    ) {
        List<StatusEntry> statusEntries = statusListEntries
                .stream()
                .map(entry -> new StatusEntry(entry.getIdx(), URI.create(entry.getUri())))
                .toList();

        return new CredentialStatusInfo(
                transaction.getCredentialIssuerTenant(),
                transaction.getCredentialConfigurationId(),
                statusEntries
        );
    }
}
