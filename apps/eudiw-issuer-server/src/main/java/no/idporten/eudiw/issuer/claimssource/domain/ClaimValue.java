package no.idporten.eudiw.issuer.claimssource.domain;

public sealed interface ClaimValue permits StringValue, NumberValue, BooleanValue, BinaryValue, DataValue, FullDateValue, DateTimeValue, ListValue, MapValue {

    Object value();
}
