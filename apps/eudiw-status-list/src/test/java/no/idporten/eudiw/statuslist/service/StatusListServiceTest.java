package no.idporten.eudiw.statuslist.service;

import no.idporten.eudiw.statuslist.exceptions.StatusListException;
import no.idporten.eudiw.statuslist.exceptions.StatusNotAllocatedException;
import no.idporten.eudiw.statuslist.exceptions.UnsupportedStatusException;
import no.idporten.eudiw.statuslist.issuer.config.StatusIssuerProperties;
import no.idporten.eudiw.statuslist.logging.audit.AuditService;
import no.idporten.eudiw.statuslist.repository.StatusListRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

import static no.idporten.eudiw.statuslist.service.Status.INVALID;
import static no.idporten.eudiw.statuslist.service.Status.VALID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatusListServiceTest {
    @Mock
    private AuditService auditService;

    @Mock
    private StatusListRepository statusListRepository;

    @Mock
    StatusIssuerProperties statusIssuerProperties;

    @InjectMocks
    private StatusListService statusListService;

    @Test
    void getLargeJsonStatuslist() {
        StatusList statusList = new StatusList("1", 1_000_000, 1);
        when(statusListRepository.getStatusList("1")).thenReturn(statusList);
        long start = System.currentTimeMillis();
        long init = System.currentTimeMillis();
        String list = statusListService.getJsonStatusList("1");
        long end = System.currentTimeMillis();
        assertNotNull(list);
        System.out.println("Init list: " + (init - start) + "ms, Generate: " + (end - init) + "ms, Total: " + (end - start) + "ms");
    }

    @Test
    void verifyGetJsonStatuslistFromSpecExample() {
        // List and result from https://drafts.oauth.net/draft-ietf-oauth-status-list/draft-ietf-oauth-status-list.html#name-compressed-byte-array and
        StatusList statusList = new StatusList("1", 16, 1);
        statusList.setStatusList(
            new HashMap<>() {{
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
            }}
        );

        when(statusListRepository.getStatusList("1")).thenReturn(statusList);

        String list = statusListService.getJsonStatusList("1");
        assertNotNull(list);
        assertEquals("78dadbb918000217015d", list, "List not equal to example=" + list);
    }

    @Test
    void getJsonStatuslistWithTwoBitsPerStatus() {
        StatusList statusList = new StatusList("1", 4, 2);
        statusList.setStatusList(new HashMap<>(Map.of(
                0, VALID,
                1, INVALID,
                2, VALID,
                3, VALID
        )));

        when(statusListRepository.getStatusList("1")).thenReturn(statusList);
        String list = statusListService.getJsonStatusList("1");

        assertEquals("78da63010000050005", list);
    }

    @Test
    void getJsonStatuslistWithEightBitsPerStatus() {
        StatusList statusList = new StatusList("1", 3, 8);
        statusList.setStatusList(new HashMap<>(Map.of(
                0, 0x12,
                1, 0xAB,
                2, 0xFF
        )));

        when(statusListRepository.getStatusList("1")).thenReturn(statusList);
        String list = statusListService.getJsonStatusList("1");

        assertEquals("78da135afd1f00028e01bd", list);
    }

    @Test
    void getJsonStatuslistRejectsStatusValueThatDoesNotFitBitSize() {
        StatusList statusList = new StatusList("1", 2, 2);
        statusList.setBitsPerStatus(2);
        statusList.setStatusList(new HashMap<>(Map.of(0, 0x04)));

        when(statusListRepository.getStatusList("1")).thenReturn(statusList);

        assertThrows(StatusListException.class, () -> statusListService.getJsonStatusList("1"));
    }

    @Test
    void updateStatusShouldThrowUnsupportedStatus() {
        assertThrows(UnsupportedStatusException.class, () -> statusListService.updateStatus("1", 0, 2));
    }

    @Test
    void updateStatusRejectsUnallocatedIndex() {
        StatusList statusList = new StatusList("1", 2, 2);
        when(statusListRepository.getStatusList("1")).thenReturn(statusList);

        assertThrows(StatusNotAllocatedException.class, () -> statusListService.updateStatus("1", 1, INVALID));
    }


    @Test
    void compressZlib() {
        // Example and values from https://drafts.oauth.net/draft-ietf-oauth-status-list/draft-ietf-oauth-status-list.html#name-compressed-byte-array
        byte[] input = new byte[]{(byte) 0xB9, (byte) 0xA3};
        byte[] compressed = StatusListService.compressZlib(input);
        assertEquals("78dadbb918000217015d", HexFormat.of().formatHex(compressed));
    }
}
