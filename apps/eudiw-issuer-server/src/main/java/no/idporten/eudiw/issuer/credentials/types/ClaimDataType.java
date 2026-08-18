package no.idporten.eudiw.issuer.credentials.types;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ClaimDataType {
    @JsonProperty("string")
    STRING("^[\\x20-\\x7EæøåÆØÅ]{1,255}$"),
    @JsonProperty("number")
    NUMBER("^\\d{1,150}$"),
    @JsonProperty("boolean")
    BOOLEAN("^(true|false)$"),
    @JsonProperty("binary")
    BINARY("^[-A-Za-z0-9+/]*={0,3}$"),
    @JsonProperty("iso_date")
    ISO_DATE("^\\d{4}-\\d{2}-\\d{2}$"),
    @JsonProperty("iso_date_time")
    ISO_DATE_TIME("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$"), // ISO_INSTANT: 2011-12-03T10:15:30Z
    @JsonProperty("list")
    LIST("^\\[.*\\]$"),
    @JsonProperty("map")
    MAP("^\\{.*\\}$");

    private final String defaultRegex;

    ClaimDataType(String defaultRegex) {
        this.defaultRegex = defaultRegex;
    }

    public String getDefaultRegex() {
        return defaultRegex;
    }

    public static ClaimDataType valueOfCaseInsensitive(String value) {
        String v = value.toUpperCase();
        return ClaimDataType.valueOf(v);
    }
}
