package no.idporten.eudiw.statuslist.util;

import no.idporten.eudiw.statuslist.exceptions.StatusListBadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static no.idporten.eudiw.statuslist.util.StatusListUtil.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StatusListUtilTest {
    @Test
    @DisplayName("Should build correct URI when given base URI and list id")
    public void isBuildingCorrectUri() {
        URI expected = URI.create("https://status.junit.eidas2sandkasse.net/lists/1");
        URI uri = buildUri("https://status.junit.eidas2sandkasse.net/lists/{id}", 1);

        assertEquals(expected, uri);

        expected = URI.create("https://status.junit.eidas2sandkasse.net/lists/123");
        uri = buildUri("https://status.junit.eidas2sandkasse.net/lists/{id}", 123);

        assertEquals(expected, uri);
    }

    @Test
    @DisplayName("Should get correct id from URI when URI ends with a valid unsigned integer")
    public void isGettingCorrectIdFromUri() {
        int expected = 1;
        int id = getListId(URI.create("https://status.junit.eidas2sandkasse.net/lists/1"));

        assertEquals(expected, id);

        expected = 123;
        id = getListId(URI.create("https://status.junit.eidas2sandkasse.net/lists/123"));

        assertEquals(expected, id);

        expected = 3;
        id = getListId(URI.create("https://status.junit.eidas2sandkasse.net/lists/en/to/3"));

        assertEquals(expected, id);
    }

    @Test
    @DisplayName("Should throw StatusListBadRequestException when URI does not end with a valid unsigned integer")
    public void isGettingIncorrectIdFromUri() {
        StatusListBadRequestException e1 = assertThrows(StatusListBadRequestException.class, () -> {
            getListId(URI.create("https://status.junit.eidas2sandkasse.net/lists/en"));
        });
        assertEquals("Unable to parse id from URI: https://status.junit.eidas2sandkasse.net/lists/en",  e1.getMessage());
        assertEquals("invalid_request",  e1.getErrorCode());

        StatusListBadRequestException e2 = assertThrows(StatusListBadRequestException.class, () -> {
            getListId(URI.create("https://status.junit.eidas2sandkasse.net/lists/-1"));
        });
        assertEquals("Unable to parse id from URI: https://status.junit.eidas2sandkasse.net/lists/-1",  e2.getMessage());
        assertEquals("invalid_request",  e2.getErrorCode());
    }

    @Test
    @DisplayName("Should generate identical lists for the same seed and size")
    public void shouldGenerateIdenticalListForGivenSeed() {
        int size = 10;
        int seed = 42;
        FreeIndexList s1 = createFreeIndexList(size, seed);
        FreeIndexList s2 = createFreeIndexList(size, seed);

        for (int i = 0; i < size; i++) {
            assertEquals(s1.peek(i), s2.peek(i));
        }

        size = 1000;
        seed = 987456;
        s1 = createFreeIndexList(size, seed);
        s2 = createFreeIndexList(size, seed);

        for (int i = 0; i < size; i++) {
            assertEquals(s1.peek(i), s2.peek(i));
        }
    }

    @Test
    @DisplayName("Should generate different lists for different seeds")
    public void shouldGenerateDifferentListsForDifferentSeed() {
        int size1 = 10;
        FreeIndexList s1 = createFreeIndexList(size1, 42);
        FreeIndexList s2 = createFreeIndexList(size1, 43);

        assertThrows(AssertionError.class, () -> {
            for (int i = 0; i < size1; i++) {
                assertEquals(s1.peek(i), s2.peek(i));
            }
        });

        int size2 = 1000;
        FreeIndexList s3 = createFreeIndexList(size2, 987456);
        FreeIndexList s4 = createFreeIndexList(size2, 654789);

        assertThrows(AssertionError.class, () -> {
            for (int i = 0; i < size2; i++) {
                assertEquals(s3.peek(i), s4.peek(i));
            }
        });
    }
}
