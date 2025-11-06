package no.idporten.eudiw.verifier.proxy.api.verification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StartVerificationRequest(

        @JsonProperty("credential_issuer")
        String credentialIssuer,

        @JsonProperty("credential_configuration_id")
        String credentialConfigurationId
) {

}
