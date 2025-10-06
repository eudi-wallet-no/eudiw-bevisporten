package no.idporten.eudiw.issuer.openid4vci.protocol;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Validated
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "Start credential issue request", title = "Start credential issue request", type = "object")
@JsonIgnoreProperties(ignoreUnknown = true)
public class StartIssuanceRequest {

    @Schema(description = "Credential issuer identifier.  See credential issuer metadata.", example = "https://utsteder.test.eidas2sandkasse.net")
    @JsonProperty("credential_issuer")
    private String credentialIssuer;

    @NotEmpty(message = "credential_configuration_id must have a value.")
    @Schema(description = "Credential configuration identifier.  See credential issuer metadata.", example = "some.known.credential_mso_mdoc")
    @JsonProperty("credential_configuration_id")
    private String credentialConfigurationId;

    @Valid
    @NotNull(message = "subject must have a value.")
    @Schema(description = "Subject for credential issuance.  The identifier must match with person identifier in access token.")
    @JsonProperty("subject")
    private Subject subject;

    @ArraySchema(schema = @Schema(implementation = Claim.class))
    @JsonProperty("claims")
    private List<Claim> claims = new ArrayList<>();

    @JsonIgnore
    public Map<String, String> getClaimsMap() {
        if (claims == null) {
            return Collections.emptyMap();
        }
        return claims.stream().collect(Collectors.toMap(Claim::getName, Claim::getValue));
    }

}
