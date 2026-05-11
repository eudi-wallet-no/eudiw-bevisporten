package no.idporten.eudiw.statuslist.service;

import no.idporten.eudiw.statuslist.exceptions.StatusListNotFoundException;
import no.idporten.eudiw.statuslist.repository.StatusListRepository;
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
        StatusList sl0 = statusListRepository.getNextFreeStatusList();
        StatusList sl1 = statusListRepository.generateNewStatusList();
        StatusList sl2 = statusListRepository.generateNewStatusList();

        assertEquals("0", sl0.getId());
        assertEquals("1", sl1.getId());
        assertEquals("2", sl2.getId());
    }


    @Test
    @DisplayName("Should return new status list with correct id")
    void getNextFreeStatusListTest() {
        StatusList sl0 = statusListRepository.getNextFreeStatusList();
        sl0.allocateToStatusList(1);
        assertEquals("0", sl0.getId());

        StatusList sl1 = statusListRepository.getNextFreeStatusList();
        assertEquals("1", sl1.getId());
        sl1.allocateToStatusList(1);

        StatusList sl2 = statusListRepository.getNextFreeStatusList();
        assertEquals("2", sl2.getId());
    }
}
