package no.idporten.eudiw.bevisgenerator.integration.issuerserver.domain;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record AuthorizationCodeGrant(
        @JsonProperty("issuer_state") String issuerState,
        @JsonProperty("authorization_server") URI authorizationServer) {
}
