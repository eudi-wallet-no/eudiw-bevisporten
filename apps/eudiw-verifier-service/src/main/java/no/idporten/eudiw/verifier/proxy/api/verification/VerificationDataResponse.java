package no.idporten.eudiw.verifier.proxy.api.verification;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record VerificationDataResponse(
        @Schema(description = "Verification transaction id.", example = "xyz...")
        @JsonProperty("verifier_transaction_id") String verifierTransactionId,
        @Schema(description = "vp_token from wallet.")
        @JsonProperty("vp_token") String vpToken,
        @Schema(description = "Verified credentials.")
        @JsonProperty("credentials") Map<String, Object> credentials
        ) {
}
