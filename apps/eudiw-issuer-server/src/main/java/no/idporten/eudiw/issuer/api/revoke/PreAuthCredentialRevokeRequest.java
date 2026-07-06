package no.idporten.eudiw.issuer.api.revoke;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

@Validated
@Schema(description = "Credential revoke request", title = "Credential revoke request", type = "object")
@JsonIgnoreProperties(ignoreUnknown = true)
public record PreAuthCredentialRevokeRequest(
        @Schema(description = "Credential configuration identifier.  See credential issuer metadata.", example = "some.known.credential_mso_mdoc")
        @JsonProperty("credential_configuration_id")
        @NotBlank(message = "credential_configuration_id must have a value")
        String credentialConfigurationId,
        @Schema(description = "Credential issuance transaction id", example = "xyz123...")
        @JsonProperty("issuance_transaction_id")
        @NotBlank(message = "issuance_transaction_id must have a value")
        String issuanceTransactionId) {
}
