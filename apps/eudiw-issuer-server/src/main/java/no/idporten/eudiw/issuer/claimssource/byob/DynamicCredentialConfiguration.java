package no.idporten.eudiw.issuer.claimssource.byob;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DynamicCredentialConfiguration {

    @JsonProperty("credential_configuration_id")
    private String credentialConfigurationId;

    @JsonProperty("vct")
    private String vct;

    @JsonProperty("credential_metadata")
    private DocumentMetadata credentialMetadata;

    @JsonProperty("format")
    private String format;

}
