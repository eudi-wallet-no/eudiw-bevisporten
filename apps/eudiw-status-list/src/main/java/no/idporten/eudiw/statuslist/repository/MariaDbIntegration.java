package no.idporten.eudiw.statuslist.repository;

import no.idporten.eudiw.statuslist.exceptions.StatusListNotFoundException;
import no.idporten.eudiw.statuslist.repository.models.StatusListDto;
import no.idporten.eudiw.statuslist.repository.models.StatusListWithEntriesDto;
import no.idporten.eudiw.statuslist.repository.rowmapper.StatusListRowMapper;
import no.idporten.eudiw.statuslist.repository.rowmapper.StatusListWithEntriesRowMapper;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Objects;
import java.util.Optional;

import static no.idporten.eudiw.statuslist.util.StatusListUtil.createNewSeed;

@Repository
public class MariaDbIntegration {

    private final StatusListProperties statusListProperties;
    private final JdbcTemplate jdbc;

    private final StatusListRowMapper statusListRowMapper;
    private final StatusListWithEntriesRowMapper statusListWithEntriesRowMapper;

    public MariaDbIntegration(
            StatusListProperties statusListProperties,
            JdbcTemplate jdbc,
            StatusListRowMapper statusListRowMapper,
            StatusListWithEntriesRowMapper statusListWithEntriesRowMapper
    ) {
        this.statusListProperties = statusListProperties;
        this.jdbc = jdbc;
        this.statusListRowMapper = statusListRowMapper;
        this.statusListWithEntriesRowMapper = statusListWithEntriesRowMapper;
    }

    public StatusListDto getStatusListById(int id) {
        Optional<StatusListDto> result = jdbc.query("""
                SELECT id, list_size, seed, next_index
                FROM status_list 
                WHERE id = ?
                """, statusListRowMapper, id).stream().findFirst();

        if (result.isEmpty()) {
            throw new StatusListNotFoundException(id);
        }

        return result.get();
    }

    public StatusListWithEntriesDto getStatusListWithEntries(int id) {
        return jdbc.query("""
                 SELECT
                    sl.id,
                    sl.list_size,
                    sle.list_index  AS list_index,
                    sle.status_value AS status_value
                FROM status_list sl
                LEFT JOIN status_list_entry sle ON sl.id = sle.status_list_id
                WHERE sl.id = ?
                """, statusListWithEntriesRowMapper, id
        );
    }

    public void updateNextIndexOnStatusList(int id, int nextIndex) {
        jdbc.update("""
                UPDATE status_list 
                SET next_index = ?, updated_ms = ?
                WHERE id = ?
                """, nextIndex, System.currentTimeMillis(), id);
    }

    public @NonNull StatusListDto getNextFreeStatusListDtoForUpdate() {
        Optional<StatusListDto> result = jdbc.query("""
                SELECT id, list_size, seed, next_index 
                FROM status_list 
                WHERE next_index < list_size
                ORDER BY id ASC
                LIMIT 1
                FOR UPDATE
                """, statusListRowMapper).stream().findFirst();

        return result.orElseGet(this::createNewStatusList);
    }

    public @NonNull StatusListDto createNewStatusList() {
        long nowMs = System.currentTimeMillis();
        StatusListDto tmp = new StatusListDto(0, statusListProperties.listSize(), createNewSeed(), 0);

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                            INSERT INTO status_list (list_size, seed, next_index, created_ms, updated_ms)
                            VALUES (?, ?, ?, ?, ?)
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setInt(1, tmp.size());
            ps.setInt(2, tmp.seed());
            ps.setInt(3, tmp.next());
            ps.setLong(4, nowMs);
            ps.setLong(5, nowMs);
            return ps;
        }, keyHolder);

        int generatedId = Objects.requireNonNull(keyHolder.getKey(), "Missing generated id").intValue();
        return new StatusListDto(generatedId, tmp.size(), tmp.seed(), tmp.next());
    }

    public Optional<Integer> getStatusListEntry(int listId, int index) {
        return jdbc.query("""
                SELECT status_value
                FROM status_list_entry
                WHERE status_list_id = ? AND list_index = ?
                FOR UPDATE
                """, (rs, rowNum) -> rs.getInt(1), listId, index).stream().findFirst();
    }

    public void upsertStatusListEntry(int listId, int index, int status) {
        long nowMs = System.currentTimeMillis();
        jdbc.update("""
                         INSERT INTO status_list_entry
                         	(status_list_id,
                         	list_index,
                         	status_value,
                         	created_ms,
                         	updated_ms)
                         VALUES
                         	(?, ?, ?, ?, ?)
                         ON DUPLICATE KEY UPDATE
                         	status_value = VALUES(status_value),
                         	updated_ms = VALUES(updated_ms)
                        """,
                listId, index, status, nowMs, nowMs
        );
    }

    public int getFreeListCount() {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) 
                FROM status_list 
                WHERE next_index < list_size
                """, Integer.class);

        return count != null ? count : 0;
    }

    public int getFullListCount() {
       Integer count = jdbc.queryForObject("""
               SELECT COUNT(*) 
               FROM status_list 
               WHERE next_index >= list_size
               """, Integer.class);

        return count != null ? count : 0;
    }
}
