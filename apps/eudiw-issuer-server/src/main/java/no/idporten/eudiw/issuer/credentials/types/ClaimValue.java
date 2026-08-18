package no.idporten.eudiw.issuer.credentials.types;

public sealed interface ClaimValue permits StringValue, NumberValue, BooleanValue, BinaryValue, FullDateValue, DateTimeValue, ListValue, MapValue {

    Object value();
}
