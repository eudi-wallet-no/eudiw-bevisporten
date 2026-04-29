package no.idporten.eudiw.statuslist.service;

import no.idporten.eudiw.statuslist.exceptions.StatusListNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StatusListRepositoryTest {
    StatusListRepository statusListRepository = new StatusListRepository(new StatusListProperties(1, 1));


    @Test
    @DisplayName("Should return status list not found when calling non-existing status-list")
    void getNonExistingStatusListTest() {
        String nonExistingId = "non-existing-id";

        assertThrows(StatusListNotFoundException.class, () -> {
            statusListRepository.getStatusList(nonExistingId);
        });
    }

    @Test
    @DisplayName("Should generate new status list with correct id")
    void getStatusListTest() {
        StatusList sl1 = statusListRepository.generateNewStatusList();
        statusListRepository.putStatusList("2", new StatusList("2"));
        StatusList sl2 = statusListRepository.generateNewStatusList();

        assertEquals("1", sl1.getId());
        assertEquals("3", sl2.getId());
    }


    @Test
    @DisplayName("Should generate new status list with correct id")
    void getNextFreeStatusListTest() {
        StatusList sl1 = new StatusList("1", 1, 1);
        sl1.allocateToStatusList(1);
        StatusList sl2 = new StatusList("2", 1, 1);
        sl2.allocateToStatusList(1);
        statusListRepository.putStatusList("1", sl1);
        statusListRepository.putStatusList("2", sl2);

        StatusList statusList = statusListRepository.getNextFreeStatusList();

        assertEquals("3", statusList.getId());
    }
}
