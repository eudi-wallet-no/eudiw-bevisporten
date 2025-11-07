package no.idporten.eudiw.issuer.claimssource.domain;

import java.util.Map;

public record ClaimMetadata (
        String name,
        // TODO path/hierarki/struktur
        String type, // String, Number, Boolean, Binary, FullDate, DateTime, List, Map
        Map<String, String> displayNames,
        boolean mandatory,
        String validationRegex
)
{
    public static final String TYPE_STRING = "string";
    public static final String TYPE_NUMBER = "number";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_BINARY = "binary";
    public static final String TYPE_FULLDATE = "fulldate"; // YYYY-MM-DD
    public static final String TYPE_DATETIME = "datetime"; // ISO 8601
    public static final String TYPE_LIST = "list";
    public static final String TYPE_MAP = "map";

    public ClaimMetadata(String name, Map<String, String> displayNames, boolean mandatory, String validationRegex) {
        this(name, TYPE_STRING, displayNames, mandatory, validationRegex);
    }
}
