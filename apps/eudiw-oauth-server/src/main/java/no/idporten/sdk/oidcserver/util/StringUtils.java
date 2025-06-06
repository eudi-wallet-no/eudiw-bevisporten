package no.idporten.sdk.oidcserver.util;

public class StringUtils {

    private StringUtils() {
    }

    /**
     * A string has text if it is not null and contains some text.
     */
    public static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

}
