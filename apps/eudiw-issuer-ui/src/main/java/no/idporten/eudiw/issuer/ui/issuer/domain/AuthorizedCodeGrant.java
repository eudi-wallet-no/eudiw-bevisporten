package no.idporten.eudiw.issuer.ui.issuer.domain;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record AuthorizedCodeGrant(@JsonProperty("issuer_state") String issuerState, @JsonProperty("authorization_server") String authorizationServer) {

}
