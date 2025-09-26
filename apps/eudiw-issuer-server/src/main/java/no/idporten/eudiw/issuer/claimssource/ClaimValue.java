package no.idporten.eudiw.issuer.claimssource;

public sealed interface ClaimValue permits StringValue {

    Object value();
}
