package no.idporten.eudiw.issuer.ui.demo.byob.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.issuer.ui.demo.web.models.advancedForm.SimpleCredentialForm;

import java.util.Collections;
import java.util.List;

public class CredentialDefinition {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("credential_configuration_id")
    private String credentialConfigurationId;

    private String vct;
    private String format;

    @JsonProperty("example_credential_data")
    private List<ExampleCredentialData> exampleCredentialData;

    @JsonProperty("credential_metadata")
    private CredentialMetadata credentialMetadata;

    public CredentialDefinition() {
    }

    public CredentialDefinition(String vct, List<ExampleCredentialData> exampleCredentialData, CredentialMetadata credentialMetadata) {
        this.vct = vct;
        this.exampleCredentialData = exampleCredentialData;
        this.credentialMetadata = credentialMetadata;
        this.format = "dc+sd-jwt";
    }

    public CredentialDefinition(SimpleCredentialForm form) {
        this.vct = form.vct();
        this.format = "dc+sd-jwt";
        this.exampleCredentialData = extractExampleData(form);
        this.credentialMetadata = extractMetadata(form);
    }

    public String getVct() {
        return vct;
    }

    public void setVct(String vct) {
        this.vct = vct;
    }

    public String getCredentialConfigurationId() {
        return credentialConfigurationId;
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


    private CredentialMetadata extractMetadata(SimpleCredentialForm form) {
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

    private List<ExampleCredentialData> extractExampleData(SimpleCredentialForm form) {
        if (form.claims() == null) {
            return Collections.emptyList();
        }

        return form
                .claims()
                .stream()
                .map(claim -> new ExampleCredentialData(claim.path(), claim.exampleValue()))
                .toList();
    }
}