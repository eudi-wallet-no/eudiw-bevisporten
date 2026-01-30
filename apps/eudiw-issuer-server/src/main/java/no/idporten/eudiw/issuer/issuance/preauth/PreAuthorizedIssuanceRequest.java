package no.idporten.eudiw.issuer.issuance.preauth;


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
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.CredentialData;
import no.idporten.eudiw.issuer.openid4vci.protocol.Claim;
import no.idporten.eudiw.issuer.openid4vci.protocol.Subject;
import org.springframework.http.HttpStatus;
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
@Schema(title = "Start credential issue request", description = "Start credential issue request.", type = "object")
@JsonIgnoreProperties(ignoreUnknown = true)
public class PreAuthorizedIssuanceRequest {

    @Schema(title = "Credential issuer identifier", description = "Identifier for the credential issuer handling the request.  See credential issuer metadata.", example = "https://utsteder.test.eidas2sandkasse.net")
    @JsonProperty("credential_issuer")
    private String credentialIssuer;

    @NotEmpty(message = "credential_configuration_id must have a value.")
    @Schema(title = "Credential configuration identifier", description = "Identifier for the credential configuration describing the credential to be issued.  See credential issuer metadata.", example = "some.known.credential_mso_mdoc")
    @JsonProperty("credential_configuration_id")
    private String credentialConfigurationId;

    @Valid
    @NotNull(message = "subject must have a value.")
    @Schema(title = "Subject for credential issuance.", description = "Identifier for the subject the credential will be issued to.  The subject must match with the person identifier in the access token.")
    @JsonProperty("subject")
    private Subject subject;

    @Schema(title = "Credential data (deprecated)", description = "Credential data as a list of claims.  Use credential_data instead.")
    @ArraySchema(schema = @Schema(implementation = Claim.class))
    @JsonProperty("claims")
    @Deprecated
    private List<Claim> claims = new ArrayList<>();

    @JsonIgnore
    public Map<String, String> getClaimsMap() {
        if (claims == null) {
            return Collections.emptyMap();
        }
        try {
            return claims
                    .stream()
                    .filter(claim -> claim.getName() != null)
                    .collect(Collectors.toMap(Claim::getName, Claim::getValue));
        } catch (IllegalStateException e) {
            throw new IssuerServerException("invalid_request", "Claims must have unique names", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            throw new IssuerServerException("invalid_request", "Invalid claims format", HttpStatus.BAD_REQUEST, e);
        }
    }

    @JsonProperty("credential_data")
    @Schema(title = "Credential data",
            description = "Credential data as a JSON object.  Use this to push credential data.")
    private Map<String, Object> credentialData;

    @JsonIgnore
    public CredentialData getCredentialData() {
        if (credentialData != null) {
            return new CredentialData(Collections.unmodifiableMap(credentialData), credentialConfigurationId);
        }
        return new CredentialData(Collections.unmodifiableMap(getClaimsMap()), getCredentialConfigurationId());
    }

}
