package no.idporten.eudiw.statuslist.util;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static no.idporten.eudiw.statuslist.util.StatusListUtil.buildUri;
import static no.idporten.eudiw.statuslist.util.StatusListUtil.getListId;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
