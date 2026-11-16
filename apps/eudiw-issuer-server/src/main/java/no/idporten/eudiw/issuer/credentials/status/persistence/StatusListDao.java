package no.idporten.eudiw.issuer.credentials.status.persistence;

import no.idporten.eudiw.issuer.credentials.status.integration.StatusEntry;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.List;

/**
 * Base class for storing token status list references linked to an issuance transaction.
 */
public abstract class StatusListDao {

    private final JdbcTemplate jdbc;
    private final String findEntriesSql;
    private final String insertEntriesSql;
    private final String deleteAllSql;

    protected StatusListDao(JdbcTemplate jdbc, String table) {
        this.jdbc = jdbc;
        this.findEntriesSql = """
                SELECT id, credential_issuance_transaction_id, uri, idx, created_ms
                FROM %s
                WHERE credential_issuance_transaction_id = ?
                ORDER BY id ASC
                """.formatted(table);
        this.insertEntriesSql = """
                INSERT INTO %s (credential_issuance_transaction_id, uri, idx, created_ms)
                VALUES (?, ?, ?, ?)
                """.formatted(table);
        this.deleteAllSql = "DELETE FROM %s".formatted(table);
    }

    public List<StatusListEntryEntity> findEntries(long credentialIssuanceTransactionId) {
        return jdbc.query(findEntriesSql,
                (rs, rowNum) -> toEntity(rs),
                credentialIssuanceTransactionId
        );
    }

    public void insertEntries(long credentialIssuanceTransactionId, List<StatusEntry> statusEntries) {
        if (statusEntries.isEmpty()) {
            return;
        }

        long createdMs = Clock.systemUTC().millis();
        jdbc.batchUpdate(insertEntriesSql,
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
        jdbc.update(deleteAllSql);
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
