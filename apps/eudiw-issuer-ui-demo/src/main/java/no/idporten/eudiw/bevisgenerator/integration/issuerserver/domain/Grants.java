package no.idporten.eudiw.bevisgenerator.integration.issuerserver.domain;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record Grants(
        @JsonProperty("urn:ietf:params:oauth:grant-type:pre-authorized_code") PreAuthorizedCodeGrant preAuthorizedCodeGrant,
        @JsonProperty("authorization_code") AuthorizationCodeGrant authorizationCodeGrant) {
}

