package no.idporten.eudiw.issuer.claimssource.domain;

public sealed interface ClaimValue permits StringValue, FullDateValue, DateTimeValue, ListValue, MapValue {

    Object value();
}
