package no.idporten.eudiw.issuer.credentials.status.persistence;

import no.idporten.eudiw.issuer.credentials.status.integration.StatusEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.List;

@Repository
public class StatusListEntryDao {

    private final JdbcTemplate jdbc;

    public StatusListEntryDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<StatusListEntryEntity> findEntries(long credentialIssuanceTransactionId) {
        return jdbc.query("""
                        SELECT id, credential_issuance_transaction_id, uri, idx, created_ms
                        FROM credential_status_list
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

        long createdMs = Clock.systemUTC().millis();
        jdbc.batchUpdate("""
                        INSERT INTO credential_status_list (credential_issuance_transaction_id, uri, idx, created_ms)
                        VALUES (?, ?, ?, ?)
                        """,
                statusEntries,
                statusEntries.size(),
                (ps, statusEntry) -> {
                    ps.setLong(1, credentialIssuanceTransactionId);
                    ps.setString(2, statusEntry.uri().toString());
                    ps.setInt(3, statusEntry.idx());
                    ps.setLong(4, createdMs);
                }
        );
    }

    public void deleteAll() {
        jdbc.update("DELETE FROM credential_status_list");
    }

    private StatusListEntryEntity toEntity(ResultSet rs) throws SQLException {
        return new StatusListEntryEntity(
                rs.getLong("id"),
                rs.getLong("credential_issuance_transaction_id"),
                rs.getString("uri"),
                rs.getInt("idx"),
                rs.getLong("created_ms")
        );
    }
}
