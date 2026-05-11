package no.idporten.eudiw.statuslist.service;

import no.idporten.eudiw.statuslist.exceptions.StatusListException;
import org.junit.jupiter.api.Test;

import java.util.*;

import static no.idporten.eudiw.statuslist.service.Status.INVALID;
import static no.idporten.eudiw.statuslist.service.Status.VALID;
import static org.junit.jupiter.api.Assertions.*;

class StatusListTest {
    StatusList statusList = new StatusList("1");


    @Test
    void setBitsPerStatusRejectsValuesOutsideSupportedRange() {
        assertThrows(StatusListException.class, () -> statusList.setBitsPerStatus(0));
        assertThrows(StatusListException.class, () -> statusList.setBitsPerStatus(9));
    }

    @Test
    void compressZlib() {
        // Example and values from https://drafts.oauth.net/draft-ietf-oauth-status-list/draft-ietf-oauth-status-list.html#name-compressed-byte-array
        byte[] input = new byte[]{(byte) 0xB9, (byte) 0xA3};
        byte[] compressed = StatusListService.compressZlib(input);
        assertEquals("78dadbb918000217015d", HexFormat.of().formatHex(compressed));
    }

    @Test
    void testUpdateStatus() {
        statusList.setStatusList(new HashMap<>(Map.of(0, VALID, 1, INVALID)));
        statusList.updateStatus(0, INVALID);
        assertEquals(INVALID, statusList.getStatusList().get(0));
    }

    @Test
    void testAllocateToStatusListHasUniqueValues() {
        List<Integer> allocatedIndices = statusList.allocateToStatusList(1_000_000);
        assertNotNull(allocatedIndices);
        Set<Integer> uniqueIndices = new HashSet<>(allocatedIndices);
        assertEquals(allocatedIndices.size(), uniqueIndices.size());
    }

    @Test
    void testStatusListShouldBeFull() {
        StatusList statusList = new StatusList("1", 1_000, 1);
        statusList.allocateToStatusList(1_000);
        assertTrue(statusList.isFull());
    }
}
