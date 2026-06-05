package no.idporten.eudiw.issuer.credentials.status.persistence;

import no.idporten.eudiw.issuer.credentials.status.integration.StatusEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class CredentialStatusDao {

    private final JdbcTemplate jdbc;

    public CredentialStatusDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<CredentialIssuanceEntity> findTransaction(
            String issuanceTransactionId,
            String credentialConfigurationId,
            String credentialIssuerTenant
    ) {
        return jdbc.query("""
                        SELECT id, issuance_transaction_id, credential_configuration_id, credential_issuer_tenant, created_ms, updated_ms
                        FROM credential_issuance_transaction
                        WHERE issuance_transaction_id = ?
                          AND credential_configuration_id = ?
                          AND credential_issuer_tenant = ?
                        """,
                (rs, rowNum) -> toCredentialIssuanceEntity(rs),
                issuanceTransactionId,
                credentialConfigurationId,
                credentialIssuerTenant
        ).stream().findFirst();
    }

    public Optional<CredentialIssuanceEntity> findTransactionByIssuanceTransactionId(
            String issuanceTransactionId,
            String credentialIssuerTenant
    ) {
        return jdbc.query("""
                        SELECT id, issuance_transaction_id, credential_configuration_id, credential_issuer_tenant, created_ms, updated_ms
                        FROM credential_issuance_transaction
                        WHERE issuance_transaction_id = ?
                          AND credential_issuer_tenant = ?
                        """,
                (rs, rowNum) -> toCredentialIssuanceEntity(rs),
                issuanceTransactionId,
                credentialIssuerTenant
        ).stream().findFirst();
    }

    public CredentialIssuanceEntity insertTransaction(
            String issuanceTransactionId,
            String credentialConfigurationId,
            String credentialIssuerTenant,
            long createdMs
    ) {
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                            INSERT INTO credential_issuance_transaction
                                (issuance_transaction_id, credential_configuration_id, credential_issuer_tenant, created_ms, updated_ms)
                            VALUES (?, ?, ?, ?, ?)
                            """);

            ps.setString(1, issuanceTransactionId);
            ps.setString(2, credentialConfigurationId);
            ps.setString(3, credentialIssuerTenant);
            ps.setLong(4, createdMs);
            ps.setLong(5, createdMs);

            return ps;
        });

        return findTransaction(issuanceTransactionId, credentialConfigurationId, credentialIssuerTenant)
                .orElseThrow(() -> new IllegalStateException("Transaction not found after insert"));
    }

    public void updateTransactionUpdatedMs(
            long transactionId,
            long updatedMs
    ) {
        int updatedRows = jdbc.update("""
                        UPDATE credential_issuance_transaction
                        SET updated_ms = ?
                        WHERE id = ?
                        """,
                updatedMs,
                transactionId
        );

        if (updatedRows != 1) {
            throw new IllegalStateException("Transaction not found for update");
        }
    }

    public List<StatusListEntryEntity> findEntries(long credentialIssuanceTransactionId) {
        return jdbc.query("""
                        SELECT id, credential_issuance_transaction_id, uri, idx
                        FROM status_list_entry
                        WHERE credential_issuance_transaction_id = ?
                        ORDER BY id ASC
                        """,
                (rs, rowNum) -> toStatusListEntryEntity(rs),
                credentialIssuanceTransactionId
        );
    }

    public void insertEntries(long credentialIssuanceTransactionId, List<StatusEntry> statusEntries) {
        if (statusEntries.isEmpty()) {
            return;
        }

        jdbc.batchUpdate("""
                        INSERT INTO status_list_entry (credential_issuance_transaction_id, uri, idx)
                        VALUES (?, ?, ?)
                        """,
                statusEntries,
                statusEntries.size(),
                (ps, statusEntry) -> {
                    ps.setLong(1, credentialIssuanceTransactionId);
                    ps.setString(2, statusEntry.uri().toString());
                    ps.setInt(3, statusEntry.idx());
                }
        );
    }

    public void deleteAllStatusListEntries() {
        jdbc.update("DELETE FROM status_list_entry");
    }

    public void deleteAllCredentialIssuanceTransactions() {
        jdbc.update("DELETE FROM credential_issuance_transaction");
    }

    private CredentialIssuanceEntity toCredentialIssuanceEntity(ResultSet rs) throws SQLException {
        return new CredentialIssuanceEntity(
                rs.getLong("id"),
                rs.getString("issuance_transaction_id"),
                rs.getString("credential_configuration_id"),
                rs.getString("credential_issuer_tenant"),
                rs.getLong("created_ms"),
                rs.getLong("updated_ms")
        );
    }

    private StatusListEntryEntity toStatusListEntryEntity(ResultSet rs) throws SQLException {
        return new StatusListEntryEntity(
                rs.getLong("id"),
                rs.getLong("credential_issuance_transaction_id"),
                rs.getString("uri"),
                rs.getInt("idx")
        );
    }
}
