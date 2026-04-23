package no.idporten.eudiw.statuslist.service;

import org.junit.jupiter.api.Test;

import java.util.*;

import static no.idporten.eudiw.statuslist.service.Status.*;
import static org.junit.jupiter.api.Assertions.*;

class StatusListServiceTest {


    @Test
    void getLargeJsonStatuslist() {
        long start = System.currentTimeMillis();
        StatusListService statuslistService = new StatusListService();
        statuslistService.setStatuslist(createRandomStatuslist(1000000));
        long init = System.currentTimeMillis();
        String list = statuslistService.getJsonStatuslist();
        long end = System.currentTimeMillis();
        assertNotNull(list);
        System.out.println("Init list: " + (init - start) + "ms, Generate: " + (end - init) + "ms, Total: " + (end - start) + "ms");
    }


    @Test
    void verifyGetJsonStatuslistFromSpecExample() {
        StatusListService statuslistService = new StatusListService();
        // List and result from https://drafts.oauth.net/draft-ietf-oauth-status-list/draft-ietf-oauth-status-list.html#name-compressed-byte-array and
        statuslistService.setStatuslist(new HashMap<>() {{
            put(0, INVALID);
            put(1, VALID);
            put(2, VALID);
            put(3, INVALID);
            put(4, INVALID);
            put(5, INVALID);
            put(6, VALID);
            put(7, INVALID);

            put(8, INVALID);
            put(9, INVALID);
            put(10, VALID);
            put(11, VALID);
            put(12, VALID);
            put(13, INVALID);
            put(14, VALID);
            put(15, INVALID);
        }});
        String list = statuslistService.getJsonStatuslist();
        assertNotNull(list);
        //System.out.println(list);
        assertEquals("78dadbb918000217015d", list, "List not equal to example=" + list);
    }

    @Test
    void getJsonStatuslistWithTwoBitsPerStatus() {
        StatusListService statuslistService = new StatusListService();
        statuslistService.setBitsPerStatus(2);
        statuslistService.setStatuslist(new HashMap<>(Map.of(
                0, VALID,
                1, INVALID,
                2, VALID,
                3, VALID
        )));

        String list = statuslistService.getJsonStatuslist();

        assertEquals("78da63010000050005", list);
    }

    @Test
    void getJsonStatuslistWithEightBitsPerStatus() {
        StatusListService statuslistService = new StatusListService();
        statuslistService.setBitsPerStatus(8);
        statuslistService.setStatuslist(new HashMap<>(Map.of(
                0, 0x12,
                1, 0xAB,
                2, 0xFF
        )));

        String list = statuslistService.getJsonStatuslist();

        assertEquals("78da135afd1f00028e01bd", list);
    }

    @Test
    void setBitsPerStatusRejectsValuesOutsideSupportedRange() {
        StatusListService statuslistService = new StatusListService();

        assertThrows(IllegalArgumentException.class, () -> statuslistService.setBitsPerStatus(0));
        assertThrows(IllegalArgumentException.class, () -> statuslistService.setBitsPerStatus(9));
    }

    @Test
    void getJsonStatuslistRejectsStatusValueThatDoesNotFitBitSize() {
        StatusListService statuslistService = new StatusListService();
        statuslistService.setBitsPerStatus(2);
        statuslistService.setStatuslist(new HashMap<>(Map.of(0, 0x04)));

        assertThrows(IllegalArgumentException.class, statuslistService::getJsonStatuslist);
    }


    @Test
    void compressZlib() {
        // Example and values from https://drafts.oauth.net/draft-ietf-oauth-status-list/draft-ietf-oauth-status-list.html#name-compressed-byte-array
        byte[] input = new byte[]{(byte) 0xB9, (byte) 0xA3};
        byte[] compressed = StatusListService.compressZlib(input);
        assertEquals("78dadbb918000217015d", HexFormat.of().formatHex(compressed));
    }

    private Map<Integer, Integer> createRandomStatuslist(int listSize) {
        Random rand = new Random();
        Map<Integer, Integer> statuslist = new HashMap<>();
        for (int i = 0; i < listSize; i++) {
            int randomStatus = rand.nextInt(2);
            statuslist.put(i, randomStatus);
        }
        return statuslist;
    }

    @Test
    void testUpdateStatus() {
        StatusListService statuslistService = new StatusListService();
        statuslistService.setStatuslist(new HashMap<>(Map.of(0, VALID, 1, INVALID)));
        statuslistService.updateStatus(0, INVALID);
        assertEquals(INVALID, statuslistService.getStatuslist().get(0));
    }

    @Test
    void testAllocateToStatusListHasUniqueValues() {
        StatusListService statuslistService = new StatusListService();

        List<Integer> allocatedIndices = statuslistService.allocateToStatusList(1_000_000);
        assertNotNull(allocatedIndices);
        Set<Integer> uniqueIndices = new HashSet<>(allocatedIndices);
        assertEquals(allocatedIndices.size(), uniqueIndices.size());
    }
}
