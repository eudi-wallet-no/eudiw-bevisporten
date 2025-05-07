package no.idporten.eudiw.issuer.openid4vci;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
@Builder
@AllArgsConstructor
public class CredentialRequest {

    @JsonProperty("credential_identifier")
    private String credentialIdentifier;

    @JsonProperty("credential_configuration_id")
    private String credentialConfigurationId;

    public void validate() {
        if (StringUtils.hasText(credentialIdentifier) && StringUtils.hasText(credentialConfigurationId)) {
            throw new InvalidCredentialRequest(InvalidCredentialRequest.INVALID_CREDENTIAL_REQUEST, "credential_identifier and credential_configuration_id can not be used is the same request.");
        }
        if (! (StringUtils.hasText(credentialIdentifier) || StringUtils.hasText(credentialConfigurationId))) {
            throw new InvalidCredentialRequest(InvalidCredentialRequest.INVALID_CREDENTIAL_REQUEST, "One of credential_identifier or credential_configuration_id must have a value.");
        }
    }

}
