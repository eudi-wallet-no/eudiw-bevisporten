package no.idporten.eudiw.issuer.claimssource.domain;

public record BinaryValue(String value, String mimeType) implements ClaimValue {

    // For MDoc, mimetype is only used for SD-JWT
    public BinaryValue (String value) {
        this(value, null);
    }
}
