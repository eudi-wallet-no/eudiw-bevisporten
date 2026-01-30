package no.idporten.eudiw.issuer.claimssource.domain;

public enum ClaimDataType {
    STRING("^[\\x20-\\x7EæøåÆØÅ]{1,255}$"),
    NUMBER("^\\d{1,150}$"),
    BOOLEAN("^(true|false)$"),
    BINARY("^[-A-Za-z0-9+/]*={0,3}$"),
    ISO_DATE("^\\d{4}-\\d{2}-\\d{2}$"),
    ISO_DATE_TIME("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$"), // ISO_INSTANT: 2011-12-03T10:15:30Z
    LIST("^\\[.*\\]$"),
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
