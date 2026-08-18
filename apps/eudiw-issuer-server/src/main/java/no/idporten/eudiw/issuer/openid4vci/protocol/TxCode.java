package no.idporten.eudiw.issuer.openid4vci.protocol;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public class TxCode {

    @JsonProperty("length")
    private int length;

    @JsonProperty("input_mode")
    private String inputMode;

    @JsonProperty("description")
    private String description;

}
