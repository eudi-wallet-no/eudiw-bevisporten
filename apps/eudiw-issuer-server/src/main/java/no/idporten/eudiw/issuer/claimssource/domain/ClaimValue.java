package no.idporten.eudiw.issuer.claimssource.domain;

public sealed interface ClaimValue permits StringValue, BooleanValue, FullDateValue, DateTimeValue, ListValue, MapValue {

    Object value();
}
