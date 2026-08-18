package no.idporten.eudiw.issuer.issuance.authz;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class SubjectCredentialTransactionDao {

    private final JdbcTemplate jdbc;

    public SubjectCredentialTransactionDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insertSubjectCredentialTransaction(
            String subjectIdentifier,
            String issuanceTransactionId,
            long createdMs
    ) {
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO subject_credential_transaction
                        (subject_identifier, issuance_transaction_id, created_ms)
                    VALUES (?, ?, ?)
                    """);
            ps.setString(1, subjectIdentifier);
            ps.setString(2, issuanceTransactionId);
            ps.setLong(3, createdMs);
            return ps;
        });
    }

    public List<SubjectCredentialIssuanceTransactionEntity> findBySubjectAndType(
            String subjectIdentifier,
            String credentialConfigurationId,
            String credentialIssuerTenant
    ) {
        return jdbc.query("""
                        SELECT s.id AS subject_credential_transaction_id,
                               s.subject_identifier,
                               s.issuance_transaction_id,
                               s.created_ms AS subject_credential_transaction_created_ms,
                               c.id AS credential_issuance_transaction_id,
                               c.credential_configuration_id,
                               c.credential_issuer_tenant,
                               c.created_ms AS credential_issuance_transaction_created_ms,
                               c.updated_ms AS credential_issuance_transaction_updated_ms,
                               c.status AS credential_issuance_transaction_status,
                               c.notification_id AS credential_issuance_transaction_notification_id
                        FROM subject_credential_transaction s
                        INNER JOIN credential_issuance_transaction c
                            ON c.issuance_transaction_id = s.issuance_transaction_id
                        WHERE s.subject_identifier = ?
                          AND c.credential_configuration_id = ?
                          AND c.credential_issuer_tenant = ?
                        ORDER BY s.created_ms DESC
                        """,
                (rs, rowNum) -> toSubjectCredentialIssuanceTransactionEntity(rs),
                subjectIdentifier,
                credentialConfigurationId,
                credentialIssuerTenant
        );
    }

    private SubjectCredentialIssuanceTransactionEntity toSubjectCredentialIssuanceTransactionEntity(ResultSet rs) throws SQLException {
        return new SubjectCredentialIssuanceTransactionEntity(
                rs.getLong("subject_credential_transaction_id"),
                rs.getString("subject_identifier"),
                rs.getString("issuance_transaction_id"),
                rs.getLong("subject_credential_transaction_created_ms"),
                rs.getLong("credential_issuance_transaction_id"),
                rs.getString("credential_configuration_id"),
                rs.getString("credential_issuer_tenant"),
                rs.getLong("credential_issuance_transaction_created_ms"),
                rs.getLong("credential_issuance_transaction_updated_ms"),
                rs.getString("credential_issuance_transaction_status"),
                rs.getString("credential_issuance_transaction_notification_id")
        );
    }

    /** Test functions */

    public void deleteAll() {
        jdbc.update("DELETE FROM subject_credential_transaction");
    }

}
