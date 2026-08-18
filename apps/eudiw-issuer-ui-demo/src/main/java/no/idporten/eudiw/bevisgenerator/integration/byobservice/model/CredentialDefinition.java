package no.idporten.eudiw.bevisgenerator.integration.byobservice.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.bevisgenerator.web.models.advancedForm.SimpleCredentialForm;

import java.util.Collections;
import java.util.List;

public class CredentialDefinition {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("credential_configuration_id")
    private String credentialConfigurationId;

    @JsonProperty("credential_type")
    private String credentialType;

    @JsonProperty("format")
    private String format;

    @JsonProperty("scope")
    private String scope;

    @JsonProperty("example_credential_data")
    private ExampleCredentialData exampleCredentialData;

    @JsonProperty("credential_metadata")
    private CredentialMetadata credentialMetadata;

    public CredentialDefinition() {
    }

    public CredentialDefinition(String credentialType, String format, String scope, ExampleCredentialData exampleCredentialData, CredentialMetadata credentialMetadata) {
        this.credentialType = credentialType;
        this.format = format;
        this.scope = scope;
        this.exampleCredentialData = exampleCredentialData;
        this.credentialMetadata = credentialMetadata;
    }

    public CredentialDefinition(SimpleCredentialForm form) {
        this(form.credentialType(), form.format(), form.scope(), extractExampleData(form), extractMetadata(form));
    }

    public String getCredentialType() {
        return credentialType;
    }

    public void setCredentialType(String credentialType) {
        this.credentialType = credentialType;
    }

    public String getCredentialConfigurationId() {
        return credentialConfigurationId;
    }

    public String getFormat() {
        return format;
    }

    public String getScope() {
        return scope;
    }

    public ExampleCredentialData getExampleCredentialData() {
        return exampleCredentialData;
    }

    public void setExampleCredentialData(ExampleCredentialData exampleCredentialData) {
        this.exampleCredentialData = exampleCredentialData;
    }

    public CredentialMetadata getCredentialMetadata() {
        return credentialMetadata;
    }

    public void setCredentialMetadata(CredentialMetadata credentialMetadata) {
        this.credentialMetadata = credentialMetadata;
    }

    private static CredentialMetadata extractMetadata(SimpleCredentialForm form) {
        List<Display> display = List.of(new Display(form.name()));

        if (form.claims() == null) {
            return new CredentialMetadata(display, Collections.emptyList());
        }

        List<Claim> claims = form
                .claims()
                .stream()
                .map(claim -> new Claim(claim.path(), claim.type(), claim.mimeType(), true, List.of(new Display(claim.name()))))
                .toList();

        return new CredentialMetadata(display, claims);
    }

    private static ExampleCredentialData extractExampleData(SimpleCredentialForm form) {
        ExampleCredentialData exampleCredentialData = new ExampleCredentialData();
        if (form.claims() == null) {
            return exampleCredentialData;
        }
        form.claims().forEach(claimForm -> exampleCredentialData.put(claimForm.path(), claimForm.exampleValue()));
        return exampleCredentialData;
    }

}