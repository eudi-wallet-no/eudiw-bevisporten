package no.idporten.sdk.oidcserver.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When using multi-valued maps")
public class MultiValuedMapUtilsTest {

    @DisplayName("then the first value for an unknown key is null")
    @Test
    void testGetFirstForUnknownKey() {
        assertNull(MultiValuedMapUtils.getFirstValue("foo", Collections.emptyMap()));
    }

    @DisplayName("then the first value for a key with an empty list is null")
    @Test
    void testGetFirstForEmptyList() {
        assertNull(MultiValuedMapUtils.getFirstValue("foo", Map.of("foo", Collections.emptyList())));
    }

    @DisplayName("then the first value for a key with a non-empty list is the first element in the list")
    @Test
    void testGetFirstForNonEmptyList() {
        assertEquals("first", MultiValuedMapUtils.getFirstValue("foo", Map.of("foo", List.of("first", "second"))));
    }

    @DisplayName("then a multi-valued map can be converted to a map containing only the first values")
    @Test
    void testConvertMultiValuedMapToMap() {
        Map<String, String> converted = MultiValuedMapUtils.toMap(Map.of("bar", Collections.emptyList(), "baz", Collections.singletonList("a"), "boz", List.of("b", "c")));
        assertAll(
                () -> assertEquals(2, converted.size()),
                () -> assertEquals("a", converted.get("baz")),
                () -> assertEquals("b", converted.get("boz"))
        );
    }

    @DisplayName("then a map can be converted to a multi-valued map")
    @Test
    void testConvertMapToMultiValuedMap() {
        Map<String, List<String>> converted = MultiValuedMapUtils.toMultiValuedMap(Map.of("foo", "bar", "fuu", "bur"));
        assertAll(
                () -> assertEquals(2, converted.size()),
                () -> assertEquals(1, converted.get("foo").size()),
                () -> assertEquals("bar", converted.get("foo").get(0)),
                () -> assertEquals(1, converted.get("fuu").size()),
                () -> assertEquals("bur", converted.get("fuu").get(0))
        );
    }

}
