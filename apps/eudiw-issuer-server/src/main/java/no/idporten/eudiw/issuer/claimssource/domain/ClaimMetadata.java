package no.idporten.eudiw.issuer.claimssource.domain;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Claim description.
 *
 * @param namespace namespace (mdoc only, first in path)
 * @param name claim name (last in path)
 * @param type claim type
 * @param displayNames display names
 * @param mandatory issuer must issue claim
 * @param validationRegex validation regex

 */
public record ClaimMetadata (
        String namespace, // only for mdoc, first in path - shallow credentials
        String name, // last in path mdoc, complete path SD-JWT VC - shallow credentials
        String type, // String, Number, Boolean, Binary, FullDate, DateTime, List, Map
        Map<String, String> displayNames,
        boolean mandatory,
        String validationRegex
)
{

    public static final String EMPTY_NAMESPACE = null;

    public static final String TYPE_STRING = "string";
    public static final String TYPE_NUMBER = "number";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_BINARY = "binary";
    public static final String TYPE_DATA = "data";
    public static final String TYPE_FULLDATE = "fulldate"; // YYYY-MM-DD
    public static final String TYPE_DATETIME = "datetime"; // ISO 8601
    public static final String TYPE_LIST = "list";
    public static final String TYPE_MAP = "map";

    /**
     * Claim description, using string as type and leaving out namespace (SD-JWT VC only).
     *
     * @param name claim name
     * @param displayNames display names
     * @param mandatory issuer must issue claim
     * @param validationRegex validation regex
     */
    public ClaimMetadata(String name, Map<String, String> displayNames, boolean mandatory, String validationRegex) {
        this(null, name, TYPE_STRING, displayNames, mandatory, validationRegex);
    }

    public String getDisplayName(String locale) {
        return displayNames.get(locale);
    }

    public List<String> path() {
        if (namespace != null) {
            return List.of(namespace, name);
        }
        return Collections.singletonList(name);
    }

}
