package no.idporten.eudiw.byob.service.model.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.byob.service.model.CredentialConfiguration;
import no.idporten.eudiw.byob.service.model.ExampleCredentialData;

import java.io.Serializable;


@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialConfigurationData(

        @JsonProperty("credential_configuration_id")
        String credentialConfigurationId,

        @JsonProperty("credential_type")
        String credentialType,

        @JsonProperty("format")
        String format,

        @JsonProperty("scope")
        String scope,

        @JsonProperty("example_credential_data")
        ExampleCredentialDataData exampleCredentialData,

        @JsonProperty("credential_metadata")
        CredentialMetadataData credentialMetadata
) implements Serializable {

    public CredentialConfigurationData(CredentialConfiguration other) {
        this(
                other.credentialConfigurationId(),
                other.credentialType(),
                other.format(),
                other.scope(),
                getExampleCredentialDataData(other.exampleCredentialData()),
                new CredentialMetadataData(other.credentialMetadata())
        );
    }

    private static ExampleCredentialDataData getExampleCredentialDataData(ExampleCredentialData exampleClaimData) {
        if (exampleClaimData == null) {
            return new ExampleCredentialDataData();
        }
        return new ExampleCredentialDataData(exampleClaimData);
    }

    public CredentialConfiguration toCredentialConfiguration() {
        return new CredentialConfiguration(
                this.credentialConfigurationId,
                this.credentialType,
                this.format,
                this.scope,
                getExampleCredentialDataData(),
                this.credentialMetadata == null ? null : this.credentialMetadata.toCredentialMetadata()
        );
    }

    private ExampleCredentialData getExampleCredentialDataData() {
        if (this.exampleCredentialData() == null) {
            return new ExampleCredentialData();
        }
        return new ExampleCredentialData(this.exampleCredentialData());
    }

}
