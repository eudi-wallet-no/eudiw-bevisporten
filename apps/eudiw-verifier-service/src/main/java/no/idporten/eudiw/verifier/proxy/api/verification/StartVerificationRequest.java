package no.idporten.eudiw.verifier.proxy.api.verification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Start verification request", title = "Start credential verification request", type = "object")
@JsonIgnoreProperties(ignoreUnknown = true)
public record StartVerificationRequest(

        @Schema(description = "Credential issuer identifier.", example = "https://utsteder.test.eidas2sandkasse.net")
        @JsonProperty("credential_issuer")
        String credentialIssuer,

        @Schema(description = "Credential configuration id.", example = "no.digdir.eudiw.pid_mso_mdoc")
        @JsonProperty("credential_configuration_id")
        String credentialConfigurationId
) {

}
