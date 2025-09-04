package no.idporten.eudiw.issuer.ui.demo.issuer.domain;


import com.fasterxml.jackson.annotation.JsonProperty;

public record PreAuthorizedCodeGrant(@JsonProperty("pre-authorized_code") String preAuthorizedCode, @JsonProperty("tx_code") TxCode txCode) {

}
