package no.idporten.eudiw.issuer.credentials.status.persistence;

import no.idporten.eudiw.issuer.credentials.status.integration.StatusEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class StatusListEntryDao {

    private final JdbcTemplate jdbc;

    public StatusListEntryDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<StatusListEntryEntity> findEntries(long credentialIssuanceTransactionId) {
        return jdbc.query("""
                        SELECT id, credential_issuance_transaction_id, uri, idx
                        FROM status_list_entry
                        WHERE credential_issuance_transaction_id = ?
                        ORDER BY id ASC
                        """,
                (rs, rowNum) -> toEntity(rs),
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

    public void deleteAll() {
        jdbc.update("DELETE FROM status_list_entry");
    }

    private StatusListEntryEntity toEntity(ResultSet rs) throws SQLException {
        return new StatusListEntryEntity(
                rs.getLong("id"),
                rs.getLong("credential_issuance_transaction_id"),
                rs.getString("uri"),
                rs.getInt("idx")
        );
    }
}
