package no.idporten.eudiw.issuer.claimssource.byob.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import no.idporten.eudiw.issuer.credentials.types.DocumentMetadata;


@Builder
public record DynamicCredentialConfiguration(

        @JsonProperty("credential_configuration_id")
        String credentialConfigurationId,

        @JsonProperty("vct")
        String vct,

        @JsonProperty("credential_metadata")
        DynamicCredentialMetadata credentialMetadata,

        @JsonProperty("format")
        String format) {


    public DocumentMetadata getCredentialMetadata() {

        return credentialMetadata.convertToDocumentMetadata();
    }


}
