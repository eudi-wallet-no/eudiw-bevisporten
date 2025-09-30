package no.idporten.eudiw.issuer.openid4vci.protocol;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Schema(description = "Start credential issue request", title = "Start credential issue request", type = "object")
@Getter
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StartIssuanceRequest {

    @Schema(description = "Credential issuer identifier.  See credential issuer metadata.", example = "https://utsteder.test.eidas2sandkasse.net")
    @JsonProperty("credential_issuer")
    private String credentialIssuer;

    @Schema(description = "Credential configuration identifier.  See credential issuer metadata.", example = "some.known.credential_mso_mdoc")
    @JsonProperty("credential_configuration_id")
    private String credentialConfigurationId;

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
