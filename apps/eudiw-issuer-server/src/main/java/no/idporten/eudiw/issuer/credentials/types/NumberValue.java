package no.idporten.eudiw.issuer.credentials.types;

// Number element for Long, Int, UInt, float, double,... We might split into different number records later
public record NumberValue(Long value) implements ClaimValue {

    public NumberValue(Integer value) {
        this(value != null ? value.longValue() : null);
    }
}
