package no.idporten.eudiw.byob.service.model.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.byob.service.model.CredentialConfiguration;
import no.idporten.eudiw.byob.service.model.ExampleCredentialData;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialConfigurationData(

        @JsonProperty("credential_configuration_id")
        String credentialConfigurationId,

        @JsonProperty("vct")
        String vct,

        @JsonProperty("format")
        String format,

        @JsonProperty("example_credential_data")
        List<ExampleCredentialDataData> exampleCredentialData,

        @JsonProperty("credential_metadata")
        CredentialMetadataData credentialMetadata
) implements Serializable {

    public CredentialConfigurationData(CredentialConfiguration other) {
        this(
                other.credentialConfigurationId(),
                other.vct(),
                other.format(),
                getExampleCredentialDataData(other.exampleCredentialData()),
                new CredentialMetadataData(other.credentialMetadata())
        );
    }

    private static List<ExampleCredentialDataData> getExampleCredentialDataData(List<ExampleCredentialData> exampleCredentialData) {
        if (exampleCredentialData == null) {
            return Collections.emptyList();
        }
        return exampleCredentialData.stream().map(example -> new ExampleCredentialDataData(example.name(), example.value())).toList();
    }

    public CredentialConfiguration toCredentialConfiguration() {
        return new CredentialConfiguration(
                this.credentialConfigurationId,
                this.vct,
                this.format,
                getExampleCredentialDataData(),
                this.credentialMetadata == null ? null : this.credentialMetadata.toCredentialMetadata()
        );
    }

    private List<ExampleCredentialData> getExampleCredentialDataData() {
        if (this.exampleCredentialData() == null) {
            return Collections.emptyList();
        }
        return this.exampleCredentialData().stream().map(ExampleCredentialDataData::toExampleCredentialData).toList();
    }
}
