package no.idporten.eudiw.statuslist.repository;

import no.idporten.eudiw.statuslist.exceptions.StatusListBadRequestException;
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
    private StatusListProperties properties;

    @Autowired
    private StatusListRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM status_list_entry");
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
    @DisplayName("Should insert new status list if no list has free space")
    void shouldInsertNewStatusListIfNoListHasFreeSpace() {
        int listSize = properties.listSize();
        String query = "SELECT count(*) FROM status_list";

        Integer count = jdbc.queryForObject(query, Integer.class);
        assertEquals(0, count);

        repository.allocateToStatusList(1);

        count = jdbc.queryForObject(query, Integer.class);
        assertEquals(1, count);

        repository.allocateToStatusList(listSize-1);

        count = jdbc.queryForObject(query, Integer.class);
        assertEquals(1, count);

        repository.allocateToStatusList(1);

        count = jdbc.queryForObject(query, Integer.class);
        assertEquals(2, count);
    }

    @Test
    @DisplayName("Should assert that the status is allocated")
    void shouldAssertStatusIsAllocated() {
        int size = 100;
        seedDbWithDummyStatusList(size);

        int allocated = 50;
        List<AllocatedIndexDto> allocatedIndexes = repository.allocateToStatusList(allocated);

        assertEquals(allocated, allocatedIndexes.size());

        int[] freeIndexList = new int[] {3, 38, 18, 55, 53, 83, 26, 46, 69, 31, 41, 35, 67, 43, 24, 16, 44, 40, 95, 22, 0, 88, 57, 90, 73, 72, 12, 8, 32, 87, 96, 19, 54, 99, 20, 85, 76, 94, 50, 92, 70, 5, 1, 6, 10, 23, 28, 48, 2, 91, 34, 81, 98, 36, 63, 42, 86, 75, 21, 82, 60, 89, 59, 52, 30, 29, 80, 17, 64, 13, 66, 49, 15, 51, 39, 47, 45, 74, 62, 97, 33, 11, 77, 61, 56, 37, 65, 93, 68, 27, 4, 79, 58, 84, 78, 14, 7, 25, 71, 9};

        int listId = allocatedIndexes.getFirst().listId();

        for (int i = 0; i < allocated; i++) {
            AllocatedIndexDto entry = allocatedIndexes.get(i);
            assertTrue(repository.isStatusAllocated(listId, entry.index()));
            assertEquals(freeIndexList[i], entry.index());
        }

        for (int i = allocated; i < size; i++) {
            assertFalse(repository.isStatusAllocated(listId, freeIndexList[i]));
        }

    }

    @Test
    @DisplayName("Should get the correct statuslist from db")
    void getStatusListById() {
        int size = 100;
        seedDbWithDummyStatusList(size);
        int allocatedCount = 5;

        List<AllocatedIndexDto> allocatedIndexes = repository.allocateToStatusList(allocatedCount);
        assertEquals(allocatedCount, allocatedIndexes.size());

        int listId = allocatedIndexes.getFirst().listId();

        StatusListWithEntriesDto statusList = repository.getStatusList(listId);
        assertNotNull(statusList);
        assertEquals(listId, statusList.id());
        assertEquals(0, statusList.entries().size());

        for (AllocatedIndexDto entry : allocatedIndexes) {
            repository.createStatusEntry(listId, entry.index(), Status.INVALID);
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

    private void seedDbWithDummyStatusList(int size) {
        long timeMs = 13413413;
        jdbc.update(
                """
                 INSERT INTO status_list (next_index, list_size, seed, created_ms, updated_ms) 
                 VALUES (?, ?, ?, ?, ?)
                 """,
                0, size, 12345, timeMs, timeMs
        );
    }

    @Test
    @DisplayName("Should throw StatusEntryException when status already revoked")
    void shouldThrowStatusEntryExceptionWhenStatusAlreadyRevoked() {
        int size = 100;
        seedDbWithDummyStatusList(size);
        int allocatedCount = 5;

        List<AllocatedIndexDto> allocatedIndexes = repository.allocateToStatusList(allocatedCount);
        assertEquals(allocatedCount, allocatedIndexes.size());

        int listId = allocatedIndexes.getFirst().listId();
        int index = allocatedIndexes.getFirst().index();

        repository.createStatusEntry(listId, index, Status.INVALID);

        StatusListBadRequestException exception = assertThrows(StatusListBadRequestException.class, () -> repository.createStatusEntry(listId, index, Status.INVALID));
        assertEquals("Status at index %d is already revoked in status list with id %d".formatted(index, listId), exception.getMessage());
        assertEquals("invalid_request", exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw StatusEntryException when status is not allocated")
    void shouldThrowStatusEntryExceptionWhenStatusIsNotAllocated() {
        int size = 20;
        seedDbWithDummyStatusList(size);

        StatusListBadRequestException exception = assertThrows(StatusListBadRequestException.class, () -> repository.createStatusEntry(1, 0, Status.INVALID));
        assertEquals("Status at index %d is not allocated in status list with id %d".formatted(0, 1),  exception.getMessage());
        assertEquals("invalid_request", exception.getErrorCode());
    }

    @Test
    @DisplayName("Should work to allocate accross several lists")
    void shouldWorkToAllocateAccrossSeveralLists() {
        List<AllocatedIndexDto> allocatedIndexes = repository.allocateToStatusList(50);

        for (AllocatedIndexDto entry : allocatedIndexes) {
            repository.createStatusEntry(entry.listId(), entry.index(), Status.INVALID);
        }

        AllocatedIndexDto first = allocatedIndexes.getFirst();
        AllocatedIndexDto lastRevoked = allocatedIndexes.get(49);
        assertThrows(StatusListBadRequestException.class, () -> repository.createStatusEntry(first.listId(), first.index(), Status.INVALID));
        assertThrows(StatusListBadRequestException.class, () -> repository.createStatusEntry(lastRevoked.listId(), lastRevoked.index(), Status.INVALID));

        assertNotEquals(first.listId(), lastRevoked.listId());
    }

    @Test
    @DisplayName("Should work for allocating 1, listSize-1 and listSize/2")
    void allocationTetst() {
        int listSize = properties.listSize();
        List<AllocatedIndexDto> allocated1 = repository.allocateToStatusList(listSize - 1);
        for  (AllocatedIndexDto entry : allocated1) {
            repository.createStatusEntry(entry.listId(), entry.index(), Status.INVALID);
        }

        AllocatedIndexDto f1 = allocated1.getFirst();
        assertThrows(StatusListBadRequestException.class, () -> repository.createStatusEntry(f1.listId(), f1.index(), Status.INVALID));

        AllocatedIndexDto l1 = allocated1.getLast();
        assertThrows(StatusListBadRequestException.class, () -> repository.createStatusEntry(l1.listId(), l1.index(), Status.INVALID));


        // Last in current list
        List<AllocatedIndexDto> allocated2 = repository.allocateToStatusList(1);
        AllocatedIndexDto f2 = allocated2.getFirst();
        repository.createStatusEntry(f2.listId(), f2.index(), Status.INVALID);
        assertThrows(StatusListBadRequestException.class, () -> repository.createStatusEntry(f2.listId(), f2.index(), Status.INVALID));

        // First in next list
        List<AllocatedIndexDto> allocated4 = repository.allocateToStatusList(1);
        AllocatedIndexDto l2 = allocated4.getLast();
        repository.createStatusEntry(l2.listId(), l2.index(), Status.INVALID);
        assertThrows(StatusListBadRequestException.class, () -> repository.createStatusEntry(l2.listId(), l2.index(), Status.INVALID));


        List<AllocatedIndexDto> allocated3 = repository.allocateToStatusList(listSize / 2 - 1);
        for  (AllocatedIndexDto entry : allocated3) {
            repository.createStatusEntry(entry.listId(), entry.index(), Status.INVALID);
        }
        AllocatedIndexDto f3 = allocated3.getFirst();
        assertThrows(StatusListBadRequestException.class, () -> repository.createStatusEntry(f3.listId(), f3.index(), Status.INVALID));

        AllocatedIndexDto l3 = allocated2.getLast();
        assertThrows(StatusListBadRequestException.class, () -> repository.createStatusEntry(l3.listId(), l3.index(), Status.INVALID));
    }
}

