package no.idporten.eudiw.issuer.api.revoke;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

@Validated
@Schema(description = "Authorization code credential revoke request", title = "Authorization code credential revoke request", type = "object")
@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthCodeCredentialRevokeRequest(
        @Schema(description = "Credential configuration identifier. See credential issuer metadata.", example = "some.known.credential_mso_mdoc")
        @JsonProperty("credential_configuration_id")
        @NotBlank(message = "credential_configuration_id must have a value")
        String credentialConfigurationId,
        @Schema(description = "Subject identifier (fnr) for credentials to revoke", example = "05821098825")
        @JsonProperty("subject_identifier")
        @NotBlank(message = "subject_identifier must have a value")
        String subjectIdentifier) {
}
