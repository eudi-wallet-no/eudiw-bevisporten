package no.idporten.eudiw.verifier.api.verification;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Verification status response", title = "Verification status response", type = "object")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record VerificationStatusResponse(

        @Schema(description = "Verification status.", examples = {"WAIT", "AVAILABLE", "UNKNOWN"})
        @JsonProperty("status") String status,
        @Schema(description = "Verification transaction id.", example = "xyz...")
        @JsonProperty("verifier_transaction_id") String verifierTransactionId

) {
}
