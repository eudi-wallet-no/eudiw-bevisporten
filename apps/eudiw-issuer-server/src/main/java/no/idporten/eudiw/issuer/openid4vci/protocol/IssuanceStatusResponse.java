package no.idporten.eudiw.issuer.openid4vci.protocol;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import no.idporten.eudiw.issuer.openid4vci.service.IssuanceTransactionId;

@Schema(description = "Credential issuance status response", title = "Issuance status response", type = "object")
@Builder
@AllArgsConstructor
public class IssuanceStatusResponse {

    @Schema(description = "Credential issuance transaction id", example = "xyz123...")
    @JsonProperty("issuance_transaction_id")
    private IssuanceTransactionId issuanceTransactionId;

    @Schema(description = "Credential issuance status", example = "offer_issued")
    @JsonProperty("status")
    private String status;

}
