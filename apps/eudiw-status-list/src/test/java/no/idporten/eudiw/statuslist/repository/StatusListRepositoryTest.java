package no.idporten.eudiw.statuslist.repository;

import no.idporten.eudiw.statuslist.repository.models.AllocatedIndexDto;
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
}
