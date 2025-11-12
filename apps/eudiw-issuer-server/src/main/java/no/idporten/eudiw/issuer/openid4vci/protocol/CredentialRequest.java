package no.idporten.eudiw.issuer.openid4vci.protocol;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CredentialRequest {

    @JsonProperty("credential_identifier")
    private String credentialIdentifier;

    @JsonProperty("credential_configuration_id")
    private String credentialConfigurationId;

    @Deprecated
    @JsonProperty("proof")
    private Proof proof;

    @JsonProperty("proofs")
    private Proofs proofs;

    public void validate() {
        if (StringUtils.hasText(credentialIdentifier) && StringUtils.hasText(credentialConfigurationId)) {
            throw new InvalidCredentialRequest(InvalidCredentialRequest.INVALID_CREDENTIAL_REQUEST, "credential_identifier and credential_configuration_id can not be used is the same request.");
        }
        if (! (StringUtils.hasText(credentialIdentifier) || StringUtils.hasText(credentialConfigurationId))) {
            throw new InvalidCredentialRequest(InvalidCredentialRequest.INVALID_CREDENTIAL_REQUEST, "One of credential_identifier or credential_configuration_id must have a value.");
        }
        if (getProof() != null) {
            proof.validate();
        }
        if (getProofs() != null) {
            proofs.validate();
        }
    }

}
