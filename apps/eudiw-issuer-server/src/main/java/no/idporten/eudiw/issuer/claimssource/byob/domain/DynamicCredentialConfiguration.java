package no.idporten.eudiw.issuer.claimssource.byob.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import no.idporten.eudiw.issuer.credentials.types.ExtendedCredentialMetadata;


@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record DynamicCredentialConfiguration(

        @JsonProperty("credential_configuration_id")
        String credentialConfigurationId,

        @JsonProperty("scope")
        String scope,

        @JsonProperty("credential_type")
        String credentialType,

        @JsonProperty("credential_metadata")
        DynamicCredentialMetadata credentialMetadata,

        @JsonProperty("format")
        String format) {


    public ExtendedCredentialMetadata toExtendedCredentialMetadata() {
        return credentialMetadata.toExtendedCredentialMetadata(this);
    }


}
