package no.idporten.eudiw.bevisgenerator.integration.issuerserver.domain;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record PreAuthorizedCodeGrant(@JsonProperty("pre-authorized_code") String preAuthorizedCode, @JsonProperty("tx_code") TxCode txCode) {

}
