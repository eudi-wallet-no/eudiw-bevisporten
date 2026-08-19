package no.idporten.eudiw.issuer.ui.issuer.domain;


import com.fasterxml.jackson.annotation.JsonProperty;

public record Grants(@JsonProperty("authorization_code") AuthorizedCodeGrant authorizedCodeGrant) {

}

