package no.idporten.eudiw.issuer.api.revoke;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.issuer.openid4vci.protocol.Subject;
import org.springframework.validation.annotation.Validated;

@Validated
@Schema(description = "Authorization code credential revoke request", title = "Authorization code credential revoke request", type = "object")
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthCodeCredentialRevokeRequest(
        @Schema(description = "Credential configuration identifier. See credential issuer metadata.", example = "some.known.credential_mso_mdoc")
        @JsonProperty("credential_configuration_id")
        @NotBlank(message = "credential_configuration_id must have a value")
        String credentialConfigurationId,
        @Schema(description = "Subject for credentials to revoke")
        @JsonProperty("subject")
        @Valid
        @NotNull(message = "subject must have a value")
        Subject subject) {
}
