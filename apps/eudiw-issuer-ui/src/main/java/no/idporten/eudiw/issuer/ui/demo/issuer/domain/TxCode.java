package no.idporten.eudiw.issuer.ui.demo.issuer.domain;


import com.fasterxml.jackson.annotation.JsonProperty;

public record TxCode(@JsonProperty("length") int length, @JsonProperty("input_mode") String inputMode, @JsonProperty("description") String description) {

}
