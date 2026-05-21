package no.idporten.eudiw.statuslist.repository;

import no.idporten.eudiw.statuslist.exceptions.ErrorCodes;
import no.idporten.eudiw.statuslist.exceptions.StatusEntryException;
import no.idporten.eudiw.statuslist.exceptions.StatusListException;
import no.idporten.eudiw.statuslist.exceptions.StatusListNotFoundException;
import no.idporten.eudiw.statuslist.repository.models.AllocatedIndexDto;
import no.idporten.eudiw.statuslist.repository.models.StatusListDto;
import no.idporten.eudiw.statuslist.repository.models.StatusListWithEntriesDto;
import no.idporten.eudiw.statuslist.service.StatusListProperties;
import no.idporten.eudiw.statuslist.util.FreeIndexList;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static no.idporten.eudiw.statuslist.util.StatusListUtil.createFreeIndexList;
import static no.idporten.eudiw.statuslist.util.StatusListUtil.createNewSeed;

@Repository
@Transactional(readOnly = true)
public class StatusListRepository {

    private final StatusListProperties statusListProperties;
    private final JdbcTemplate jdbc;
    private final Map<FreeIndexKey, FreeIndexList> freeIndexLists = new ConcurrentHashMap<>();

    public StatusListRepository(StatusListProperties statusListProperties, JdbcTemplate jdbc) {
        this.statusListProperties = statusListProperties;
        this.jdbc = jdbc;
    }

    public StatusListWithEntriesDto getStatusList(String listId) {
        try {
            int id = Integer.parseInt(listId);
            return getStatusList(id);
        } catch (NumberFormatException e) {
            throw new StatusListNotFoundException(listId);
        }
    }

    public StatusListWithEntriesDto getStatusList(int listId) {
        StatusListWithEntriesDto result = getStatusListWithEntries(listId);

        if (result == null) {
            throw new StatusListNotFoundException(listId);
        }

        return result;
    }

    public StatusListDto getStatusListById(int id) {
        Optional<StatusListDto> result = jdbc.query("""
                        SELECT id, list_size, seed, next_index
                        FROM status_list 
                        WHERE id = ?
                        ORDER BY id ASC
                        LIMIT 1
                        """,
                (rs, i) -> new StatusListDto(
                        rs.getInt(1),
                        rs.getInt(2),
                        rs.getInt(3),
                        rs.getInt(4)
                ), id).stream().findFirst();

        if (result.isEmpty()) {
            throw new StatusListNotFoundException(id);
        }

        return result.get();
    }

    @Transactional
    public void createStatusEntry(int listId, int index, int status) {
        if (!isStatusAllocated(listId, index)) {
            throw new StatusEntryException(
                    ErrorCodes.STATUS_NOT_ALLOCATED,
                    "Status at index %d is not allocated in status list with id %d".formatted(index, listId)
            );
        }

        createStatusListEntry(listId, index, status);
    }

    public boolean isStatusAllocated(int listId, int index) {
        StatusListDto statusList = getStatusListById(listId);
        FreeIndexList freeIndexList = getOrCreateFreeIndexList(statusList);
        return freeIndexList.isAllocated(index, statusList.next());
    }

    @Transactional
    public List<AllocatedIndexDto> allocateToStatusList(int count) {
        List<AllocatedIndexDto> allocatedIndexes = new ArrayList<>(count);
        int remaining = count;
        int counter = 0;

        while (remaining > 0) {
            counter++;
            if (counter >= 100) {
                throw new StatusListException("Failed to allocate indexes after %d attempts".formatted(counter));
            }

            StatusListDto statusList = getNextFreeStatusListDtoForUpdate();
            FreeIndexList indexList = getOrCreateFreeIndexList(statusList);

            int free = statusList.size() - statusList.next();
            int take = Math.min(free, remaining);
            int from = statusList.next();
            int to = from + take;

            int[] indexes = indexList.getRange(from, to);
            remaining -= take;

            for (int i : indexes) {
                allocatedIndexes.add(new AllocatedIndexDto(statusList.id(), i));
            }

            int next = statusList.next() + take;
            updateNextIndexOnStatusList(statusList.id(), next);
        }

        return allocatedIndexes;
    }

    private StatusListWithEntriesDto getStatusListWithEntries(int id) {
        return jdbc.query("""
                         SELECT
                            sl.id,
                            sl.list_size,
                            sle.list_index  AS list_index,
                            sle.status_value AS status_value
                        FROM status_list sl
                        LEFT JOIN status_list_entry sle ON sl.id = sle.status_list_id
                        WHERE sl.id = ?
                        """,
                rs -> {
                    StatusListWithEntriesDto sl = null;
                    while (rs.next()) {
                        if (sl == null) {
                            sl = new StatusListWithEntriesDto(
                                    rs.getInt("id"),
                                    rs.getInt("list_size"),
                                    statusListProperties.bitsPerStatus(),
                                    new HashMap<>()
                            );
                        }

                        Integer listIndex = rs.getObject("list_index", Integer.class);
                        Integer statusValue = rs.getObject("status_value", Integer.class);
                        if (listIndex != null && statusValue != null) {
                            sl.entries().put(listIndex, statusValue);
                        }
                    }

                    return sl;
                },
                id
        );
    }

    private FreeIndexList getOrCreateFreeIndexList(StatusListDto statusListDto) {
        FreeIndexKey key = new FreeIndexKey(statusListDto.id(), statusListDto.size(), statusListDto.seed());
        return freeIndexLists.computeIfAbsent(
                key,
                _ -> createFreeIndexList(statusListDto.size(), statusListDto.seed())
        );
    }

    private void updateNextIndexOnStatusList(int id, int nextIndex) {
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    UPDATE status_list 
                    SET next_index = ?, updated_ms = ?
                    WHERE id = ?
                    """);
            ps.setInt(1, nextIndex);
            ps.setLong(2, System.currentTimeMillis());
            ps.setInt(3, id);

            return ps;
        });
    }

    private @NonNull StatusListDto getNextFreeStatusListDtoForUpdate() {
        Optional<StatusListDto> result = jdbc.query("""
                        SELECT id, list_size, seed, next_index 
                        FROM status_list 
                        WHERE next_index < list_size
                        ORDER BY id ASC
                        LIMIT 1
                        FOR UPDATE
                        """,
                (rs, i) -> new StatusListDto(
                        rs.getInt(1),
                        rs.getInt(2),
                        rs.getInt(3),
                        rs.getInt(4)
                )).stream().findFirst();

        return result.orElseGet(this::createNewStatusList);
    }

    @NonNull StatusListDto createNewStatusList() {
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

    private void createStatusListEntry(int listId, int index, int status) {
        try {
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
                            """,
                    listId, index, status, nowMs, nowMs
            );
        } catch (DuplicateKeyException ex) {
            throw new StatusEntryException(
                    ErrorCodes.STATUS_ALREADY_REVOKED,
                    "Status at index %d is already revoked in status list with id %d".formatted(index, listId),
                    ex
            );
        }
    }
}
