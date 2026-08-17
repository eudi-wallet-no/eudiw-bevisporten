package no.idporten.eudiw.byob.service.model.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.byob.service.model.CredentialMetadata;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialMetadataData(
        @JsonProperty("display")
        List<DisplayData> display,

        @JsonProperty("claims")
        List<ClaimsData> claims
) implements Serializable {

    public CredentialMetadataData(CredentialMetadata credentialMetadata) {
        this(credentialMetadata.display().stream().map(DisplayData::new).toList(), credentialMetadata.claims().stream().map(ClaimsData::new).toList());
    }

    public CredentialMetadata toCredentialMetadata() {
        return new CredentialMetadata(
                this.display == null ? Collections.emptyList() : this.display.stream().map(DisplayData::toDisplay).toList(),
                this.claims == null ? Collections.emptyList() : this.claims.stream().map(ClaimsData::toClaims).toList()
        );
    }
}
