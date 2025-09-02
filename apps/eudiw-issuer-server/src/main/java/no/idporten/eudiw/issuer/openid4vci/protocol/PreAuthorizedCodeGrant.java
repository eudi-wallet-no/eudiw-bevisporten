package no.idporten.eudiw.issuer.openid4vci.protocol;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public class PreAuthorizedCodeGrant {

    @JsonProperty("pre-authorized_code")
    private String preAuthorizedCode;

    @JsonProperty("tx_code")
    private TxCode txCode;

}
