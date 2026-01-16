package no.idporten.eudiw.issuer.ui.demo.byob.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class CredentialDefinitionCollection {
    @JsonProperty("credential_configurations")
    public List<CredentialDefinition> credentialConfigurations;
}
