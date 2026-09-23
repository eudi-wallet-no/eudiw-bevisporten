package no.idporten.eudiw.oauth2.server.util;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When handling URIs")
public class URIUtilsTest {

    @DisplayName("When query parameters are appended, then each parameter is URL-encoded once")
    @Test
    void testAppendQuery() {
        URI base = URI.create("https://junit.digdir.no/callback");
        Map<String, String> parameters = Map.of(
                "state", "hawaii",
                "iss", "https://auth.test.eidas2sandkasse.net");
        URI constructed = URIUtils.appendQuery(base, parameters);
        assertAll(
                () -> assertTrue(constructed.toString().startsWith("https://junit.digdir.no/callback?")),
                () -> assertTrue(constructed.toString().contains("state=hawaii")),
                () -> assertTrue(constructed.getRawQuery().contains("iss=https%3A%2F%2Fauth.test.eidas2sandkasse.net")),
                () -> assertFalse(constructed.getRawQuery().contains("%25")),
                () -> assertTrue(constructed.toString().contains("&"))
        );
    }

    @DisplayName("then a query can be parsed into a multi-valued map")
    @Test
    void testParseQuery() {
        Map<String, List<String>> parsedQuery = URIUtils.parseParameters("state=hawaii&code=secret");
        assertAll(
                () -> assertEquals(2, parsedQuery.size()),
                () -> assertEquals("hawaii", parsedQuery.get("state").get(0)),
                () -> assertEquals("secret", parsedQuery.get("code").get(0))
        );
    }

    @DisplayName("then a path can be appended")
    @Test
    void testAppendPath() {
        assertEquals("https://junit.digdir.no/callback/test", URIUtils.appendPath(URI.create("https://junit.digdir.no/"), "callback/test").toString());
        assertEquals("https://junit.digdir.no/callback/test", URIUtils.appendPath(URI.create("https://junit.digdir.no"), "/callback/test").toString());
        assertEquals("https://junit.digdir.no/callback/test", URIUtils.appendPath(URI.create("https://junit.digdir.no/"), "/callback/test").toString());
        assertEquals("https://junit.digdir.no/callback/test", URIUtils.appendPath(URI.create("https://junit.digdir.no"), "callback/test").toString());
    }

}
