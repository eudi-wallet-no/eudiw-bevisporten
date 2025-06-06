package no.idporten.sdk.oidcserver.util;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When handling URIs")
public class URIUtilsTest {

    @DisplayName("then query parameters can be appended to a base uri")
    @Test
    void testAppendQuery() {
        URI base = URI.create("https://junit.digdir.no/callback");
        Map<String, String> parameters = Map.of("state", "hawaii", "code", "secret");
        URI constructed = URIUtils.appendQuery(base, parameters);
        assertAll(
                () -> assertTrue(constructed.toString().startsWith("https://junit.digdir.no/callback?")),
                () -> assertTrue(constructed.toString().contains("state=hawaii")),
                () -> assertTrue(constructed.toString().contains("code=secret")),
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
