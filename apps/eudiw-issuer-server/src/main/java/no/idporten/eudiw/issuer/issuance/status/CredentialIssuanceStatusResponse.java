package no.idporten.eudiw.issuer.issuance.status;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;

@Schema(description = "Credential issuance status response", title = "Issuance status response", type = "object")
@Builder
@AllArgsConstructor
public class CredentialIssuanceStatusResponse {

    @Schema(description = "Credential issuance transaction id", example = "xyz123...")
    @JsonProperty("issuance_transaction_id")
    private IssuanceTransactionId issuanceTransactionId;

    @Schema(description = "Credential issuance status set by issuer or notified from wallet",
            examples = {"offer_issued", "credential_issued", "credential_accepted", "credential_deleted", "credential_failure", "unknown"})
    @JsonProperty("status")
    private String status;

}
