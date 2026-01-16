package no.idporten.eudiw.issuer.ui.demo.byob.model;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class CredentialDefinition {
    @JsonProperty("credential_configuration_id")
    private String credentialConfigurationId;
    private String vct;
    private String format;

    @JsonProperty("example_credential_data")
    private List<ExampleCredentialData> exampleCredentialData;

    @JsonProperty("credential_metadata")
    private CredentialMetadata credentialMetadata;

    public CredentialDefinition() {}

    public CredentialDefinition(String vct, List<ExampleCredentialData> exampleCredentialData, CredentialMetadata credentialMetadata) {
        this.vct = vct;
        this.exampleCredentialData = exampleCredentialData;
        this.credentialMetadata = credentialMetadata;
        this.format = "dc+sd-jwt";
    }

    public String getVct() {
        return vct;
    }

    public void setVct(String vct) {
        this.vct = vct;
    }

    public String getFormat() {
        return format;
    }

    public List<ExampleCredentialData> getExampleCredentialData() {
        return exampleCredentialData;
    }

    public void setExampleCredentialData(List<ExampleCredentialData> exampleCredentialData) {
        this.exampleCredentialData = exampleCredentialData;
    }

    public CredentialMetadata getCredentialMetadata() {
        return credentialMetadata;
    }

    public void setCredentialMetadata(CredentialMetadata credentialMetadata) {
        this.credentialMetadata = credentialMetadata;
    }
}