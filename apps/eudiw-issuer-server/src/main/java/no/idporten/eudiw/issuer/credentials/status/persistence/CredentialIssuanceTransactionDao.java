package no.idporten.eudiw.issuer.credentials.status.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class CredentialIssuanceTransactionDao {

    private final JdbcTemplate jdbc;

    public CredentialIssuanceTransactionDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<CredentialIssuanceTransactionEntity> findTransaction(
            String issuanceTransactionId,
            String credentialConfigurationId,
            String credentialIssuerTenant
    ) {
        return jdbc.query("""
                        SELECT id, issuance_transaction_id, credential_configuration_id, credential_issuer_tenant, created_ms, updated_ms, status, notification_id, revoked_ms
                        FROM credential_issuance_transaction
                        WHERE issuance_transaction_id = ?
                          AND credential_configuration_id = ?
                          AND credential_issuer_tenant = ?
                        """,
                (rs, rowNum) -> toEntity(rs),
                issuanceTransactionId,
                credentialConfigurationId,
                credentialIssuerTenant
        ).stream().findFirst();
    }

    public Optional<CredentialIssuanceTransactionEntity> findByIssuanceTransactionId(
            String issuanceTransactionId,
            String credentialIssuerTenant
    ) {
        return jdbc.query("""
                        SELECT id, issuance_transaction_id, credential_configuration_id, credential_issuer_tenant, created_ms, updated_ms, status, notification_id, revoked_ms
                        FROM credential_issuance_transaction
                        WHERE issuance_transaction_id = ?
                          AND credential_issuer_tenant = ?
                        """,
                (rs, rowNum) -> toEntity(rs),
                issuanceTransactionId,
                credentialIssuerTenant
        ).stream().findFirst();
    }

    public Optional<CredentialIssuanceTransactionEntity> findByNotificationId(String notificationId) {
        return jdbc.query("""
                        SELECT id, issuance_transaction_id, credential_configuration_id, credential_issuer_tenant, created_ms, updated_ms, status, notification_id, revoked_ms
                        FROM credential_issuance_transaction
                        WHERE notification_id = ?
                        """,
                (rs, rowNum) -> toEntity(rs),
                notificationId
        ).stream().findFirst();
    }

    public CredentialIssuanceTransactionEntity insertTransaction(
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

    public void updateStatus(String issuanceTransactionId, String status, long updatedMs) {
        jdbc.update("""
                        UPDATE credential_issuance_transaction
                        SET status = ?, updated_ms = ?
                        WHERE issuance_transaction_id = ?
                        """,
                status,
                updatedMs,
                issuanceTransactionId
        );
    }

    public void updateNotificationId(String issuanceTransactionId, String notificationId, long updatedMs) {
        jdbc.update("""
                        UPDATE credential_issuance_transaction
                        SET notification_id = ?, updated_ms = ?
                        WHERE issuance_transaction_id = ?
                        """,
                notificationId,
                updatedMs,
                issuanceTransactionId
        );
    }

    public void updateRevokedMs(String issuanceTransactionId, String credentialIssuerTenant, long revokedMs, long updatedMs) {
        jdbc.update("""
                        UPDATE credential_issuance_transaction
                        SET revoked_ms = ?, updated_ms = ?
                        WHERE issuance_transaction_id = ?
                          AND credential_issuer_tenant = ?
                        """,
                revokedMs,
                updatedMs,
                issuanceTransactionId,
                credentialIssuerTenant
        );
    }

    public void updateUpdatedMs(long id, long updatedMs) {
        int updatedRows = jdbc.update("""
                        UPDATE credential_issuance_transaction
                        SET updated_ms = ?
                        WHERE id = ?
                        """,
                updatedMs,
                id
        );

        if (updatedRows != 1) {
            throw new IllegalStateException("Transaction not found for update");
        }
    }

    private CredentialIssuanceTransactionEntity toEntity(ResultSet rs) throws SQLException {
        return new CredentialIssuanceTransactionEntity(
                rs.getLong("id"),
                rs.getString("issuance_transaction_id"),
                rs.getString("credential_configuration_id"),
                rs.getString("credential_issuer_tenant"),
                rs.getLong("created_ms"),
                rs.getLong("updated_ms"),
                rs.getString("status"),
                rs.getString("notification_id"),
                rs.getObject("revoked_ms", Long.class)
        );
    }

    /** Test functions */
    
    public Optional<CredentialIssuanceTransactionEntity> findByIssuanceTransactionId(String issuanceTransactionId) {
        return jdbc.query("""
                        SELECT id, issuance_transaction_id, credential_configuration_id, credential_issuer_tenant, created_ms, updated_ms, status, notification_id, revoked_ms
                        FROM credential_issuance_transaction
                        WHERE issuance_transaction_id = ?
                        """,
                (rs, rowNum) -> toEntity(rs),
                issuanceTransactionId
        ).stream().findFirst();
    }

    public void deleteAll() {
        jdbc.update("DELETE FROM credential_issuance_transaction");
    }

}
