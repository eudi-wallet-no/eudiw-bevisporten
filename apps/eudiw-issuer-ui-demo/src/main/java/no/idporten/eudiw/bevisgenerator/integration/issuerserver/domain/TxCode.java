package no.idporten.eudiw.bevisgenerator.integration.issuerserver.domain;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record TxCode(@JsonProperty("length") int length, @JsonProperty("input_mode") String inputMode, @JsonProperty("description") String description) {

}
