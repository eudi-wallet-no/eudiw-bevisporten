package no.idporten.eudiw.statuslist.repository;

import no.idporten.eudiw.statuslist.exceptions.StatusListNotFoundException;
import no.idporten.eudiw.statuslist.repository.models.AllocatedIndexDto;
import no.idporten.eudiw.statuslist.repository.models.StatusListWithEntriesDto;
import no.idporten.eudiw.statuslist.service.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("StatusListRepository with H2 -")
class StatusListRepositoryTest {

    @Autowired
    private StatusListRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM status_list");
        jdbc.update("DELETE FROM status_list_entry");
        jdbc.execute("ALTER TABLE status_list ALTER COLUMN id RESTART WITH 1");
    }

    @Test
    @DisplayName("allocateIndexes should return allocated indexes and update next_index in database")
    void allocateWithinSingleList() {
        List<AllocatedIndexDto> allocated = repository.allocateToStatusList(4);

        assertEquals(4, allocated.size());
        assertEquals(1, allocated.stream().map(AllocatedIndexDto::listId).distinct().count());

        int listId = allocated.getFirst().listId();
        Integer nextIndex = jdbc.queryForObject(
                "SELECT next_index FROM status_list WHERE id = ?",
                Integer.class,
                listId
        );

        assertNotNull(nextIndex);
        assertEquals(4, nextIndex);
    }

    @Test
    @DisplayName("allocateIndexes should allocate in more than one list if not enough free indexes")
    void allocateAcrossMultipleLists() {
        List<AllocatedIndexDto> allocated = repository.allocateToStatusList(14);

        assertEquals(14, allocated.size());

        Set<Integer> listIds = allocated.stream()
                .map(AllocatedIndexDto::listId)
                .collect(Collectors.toSet());

        assertEquals(2, listIds.size(), "Expected allocation to span two status lists");

        // No duplicate (listId,index) pairs
        long distinctPairs = allocated.stream()
                .map(a -> a.listId() + ":" + a.index())
                .distinct()
                .count();
        assertEquals(allocated.size(), distinctPairs);

        for (int id : listIds) {
            Integer nextIndex = jdbc.queryForObject(
                    "SELECT next_index FROM status_list WHERE id = ?",
                    Integer.class,
                    id
            );
            assertNotNull(nextIndex);
            assertTrue(nextIndex >= 0 && nextIndex <= 10);
        }
    }

    @Test
    @DisplayName("Should create new StatusListDto instance and insert into database when allocating from empty repository")
    void createNew() {
        String query = "SELECT count(*) FROM status_list";
        Integer count = jdbc.queryForObject(query, Integer.class);
        assertEquals(0, count);

        repository.createNewStatusList();
        count = jdbc.queryForObject(query, Integer.class);
        assertEquals(1, count);
    }

    @Test
    @DisplayName("Should get the correct statuslist from db")
    void getStatusListById() {
        long timeMs = 13413413;
        jdbc.update(
                """
                 INSERT INTO status_list (next_index, list_size, seed, created_ms, updated_ms) 
                 VALUES (?, ?, ?, ?, ?)
                 """,
                0, 100, 12345, timeMs, timeMs
        );

        int allocatedCount = 5;

        List<AllocatedIndexDto> allocatedIndexes = repository.allocateToStatusList(allocatedCount);
        assertEquals(allocatedCount, allocatedIndexes.size());

        int listId = allocatedIndexes.getFirst().listId();

        StatusListWithEntriesDto statusList = repository.getStatusList(listId);
        assertNotNull(statusList);
        assertEquals(listId, statusList.id());
        assertEquals(0, statusList.entries().size());

        for (AllocatedIndexDto entry : allocatedIndexes) {
           revoke(entry.listId(), entry.index());
        }

        statusList = repository.getStatusList(listId);
        assertNotNull(statusList);
        assertEquals(listId, statusList.id());
        assertEquals(allocatedCount, statusList.entries().size());

        for (AllocatedIndexDto entry : allocatedIndexes) {
            assertEquals(Status.INVALID, statusList.entries().get(entry.index()));
        }
    }

    @Test
    @DisplayName("Should throw StatusListNotFoundException when getting non-existing status list")
    void getStatusListNotFound() {
        int nonExistingId = 999;
        assertThrows(StatusListNotFoundException.class, () -> repository.getStatusList(nonExistingId));
    }

    private void revoke(int listId, int index) {
        jdbc.update("""
                    INSERT INTO status_list_entry (status_list_id, list_index, status_value)
                    VALUES (?, ?, ?)
            """, listId, index, Status.INVALID);
    }
}
