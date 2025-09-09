package no.idporten.eudiw.issuer.ui.issuer.domain;


import com.fasterxml.jackson.annotation.JsonProperty;

public record Grants(@JsonProperty("urn:ietf:params:oauth:grant-type:pre-authorized_code") PreAuthorizedCodeGrant preAuthorizedCodeGrant) {

}

