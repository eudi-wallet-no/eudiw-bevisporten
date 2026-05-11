package no.idporten.eudiw.statuslist.util;

import no.idporten.eudiw.statuslist.service.IntStack;
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
        IntStack s1 = createFreeIndexStack(size, seed);
        IntStack s2 = createFreeIndexStack(size, seed);

        for (int i = 0; i < size; i++) {
            assertEquals(s1.pop(), s2.pop());
        }

        size = 1000;
        seed = 987456;
        s1 = createFreeIndexStack(size, seed, 569);
        s2 = createFreeIndexStack(size, seed, 569);

        for (int i = 0; i < 569; i++) {
            assertEquals(s1.pop(), s2.pop());
        }
    }

    @Test
    public void shouldGenerateDifferentListsForDifferentSeed() {
        int size = 10;
        IntStack s1 = createFreeIndexStack(size, 42);
        IntStack s2 = createFreeIndexStack(size, 43);

        assertThrows(AssertionError.class, () -> {
            for (int i = 0; i < size; i++) {
                assertEquals(s1.pop(), s2.pop());
            }
        });

        IntStack s3 = createFreeIndexStack(1000, 987456, 569);
        IntStack s4 = createFreeIndexStack(1000, 654789, 569);

        assertThrows(AssertionError.class, () -> {
            for (int i = 0; i < 569; i++) {
                assertEquals(s3.pop(), s4.pop());
            }
        });
    }
}
