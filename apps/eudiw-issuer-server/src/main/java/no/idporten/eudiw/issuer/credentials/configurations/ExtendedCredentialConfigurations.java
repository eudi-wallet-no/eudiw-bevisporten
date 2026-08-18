package no.idporten.eudiw.issuer.credentials.configurations;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExtendedCredentialConfigurations {

    @JsonProperty("credential_configurations")
    private List<ExtendedCredentialConfiguration> credentialConfigurations;

}
