package no.idporten.eudiw.verifier.proxy.openid4vp.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CredentialConfiguration {

    @JsonProperty("format")
    private String format;

    @JsonProperty("vct")
    private String vct;

    @JsonProperty("doctype")
    private String doctype;

    @JsonProperty("credential_metadata")
    private CredentialMetadata credentialMetadata;

}
