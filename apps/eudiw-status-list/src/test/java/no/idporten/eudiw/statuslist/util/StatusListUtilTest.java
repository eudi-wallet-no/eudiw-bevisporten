package no.idporten.eudiw.statuslist.util;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static no.idporten.eudiw.statuslist.util.StatusListUtil.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StatusListUtilTest {
    @Test
    public void isBuildingCorrectUri() {
        URI expected = URI.create("https://status.junit.eidas2sandkasse.net/lists/1");
        URI uri = buildUri("https://status.junit.eidas2sandkasse.net/lists/{id}", "1");

        assertEquals(expected, uri);

        expected = URI.create("https://status.junit.eidas2sandkasse.net/lists/en-to-tre");
        uri = buildUri("https://status.junit.eidas2sandkasse.net/lists/{id}", "en-to-tre");

        assertEquals(expected, uri);
    }

    @Test
    public void isGettingCorrectIdFromUri() {
        String expected = "1";
        String id = getListId(URI.create("https://status.junit.eidas2sandkasse.net/lists/1"));

        assertEquals(expected, id);

        expected = "en-to-tre";
        id = getListId(URI.create("https://status.junit.eidas2sandkasse.net/lists/en-to-tre"));

        assertEquals(expected, id);

        expected = "tre";
        id = getListId(URI.create("https://status.junit.eidas2sandkasse.net/lists/en/to/tre"));

        assertEquals(expected, id);
    }

    @Test
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
