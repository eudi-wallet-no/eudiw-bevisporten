package no.idporten.eudiw.issuer.credentials.status.persistence;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.credentials.status.cache.CredentialStatusInfo;
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

    private final CredentialStatusDao credentialStatusDao;

    @Transactional
    public void storeCredentialStatus(CredentialIssuerTenant tenant, IssuanceTransactionId transactionId, CredentialStatusInfo credentialStatusInfo) {
        long now = System.currentTimeMillis();

        CredentialIssuanceEntity transaction = credentialStatusDao.insertTransaction(
                transactionId.getValue(),
                credentialStatusInfo.credentialConfigurationId(),
                tenant.getId(),
                now
        );

        credentialStatusDao.insertEntries(transaction.getId(), credentialStatusInfo.statusEntries());
    }

    @Transactional(readOnly = true)
    public CredentialStatusInfo retrieveCredentialStatus(CredentialIssuerTenant tenant, IssuanceTransactionId transactionId) {
        Optional<CredentialIssuanceEntity> transaction = credentialStatusDao.findTransactionByIssuanceTransactionId(
                transactionId.getValue(),
                tenant.getId()
        );

        if (transaction.isEmpty()) {
            return null;
        }

        CredentialIssuanceEntity transactionEntity = transaction.get();
        List<StatusListEntryEntity> statusListEntries = credentialStatusDao.findEntries(transactionEntity.getId());

        return toCredentialStatusInfo(transactionEntity, statusListEntries);
    }

    @Transactional(readOnly = true)
    public CredentialStatusInfo retrieveCredentialStatus(CredentialIssuerTenant tenant, IssuanceTransactionId transactionId, String credentialConfigurationId) {
        Optional<CredentialIssuanceEntity> transaction = credentialStatusDao.findTransaction(
                transactionId.getValue(),
                credentialConfigurationId,
                tenant.getId()
        );

        if (transaction.isEmpty()) {
            return null;
        }

        CredentialIssuanceEntity transactionEntity = transaction.get();
        List<StatusListEntryEntity> statusListEntries = credentialStatusDao.findEntries(transactionEntity.getId());

        return toCredentialStatusInfo(transactionEntity, statusListEntries);
    }

    private CredentialStatusInfo toCredentialStatusInfo(
            CredentialIssuanceEntity transaction,
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
