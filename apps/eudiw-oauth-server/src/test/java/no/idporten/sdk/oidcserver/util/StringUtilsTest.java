package no.idporten.sdk.oidcserver.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("When using string utilities")
public class StringUtilsTest {

    @DisplayName("then null or empty strings does not have text")
    @Test
    void testNullAndEmptyStrings() {
        assertFalse(StringUtils.hasText(null));
        assertFalse(StringUtils.hasText(""));
        assertFalse(StringUtils.hasText("   "));
    }

    @DisplayName("then text is detected")
    @Test
    void testHAsText() {
        assertTrue(StringUtils.hasText("null"));
        assertTrue(StringUtils.hasText("foo"));
        assertTrue(StringUtils.hasText("   foo bar   "));
    }

}
