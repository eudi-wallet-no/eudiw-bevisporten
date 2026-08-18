package no.idporten.eudiw.issuer.api.issuance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@Schema(description = "Credential offer request", title = "Credential offer request", type = "object")
@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialOfferRequest(
        @NotEmpty(message = "credential_configuration_ids must have a value.")
        @Schema(description = "Credential configuration identifiers.  See credential issuer metadata.",
                example = """
                        ["some.known.credential_mso_mdoc","some.known.credential_sd_jwt_vc"]
                        """)
        @JsonProperty("credential_configuration_ids")
        List<@NotBlank(message = "A credential_configuration_id must have a value") String> credentialConfigurationIds) {
}
