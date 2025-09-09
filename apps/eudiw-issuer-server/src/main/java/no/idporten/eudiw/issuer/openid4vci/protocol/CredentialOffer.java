package no.idporten.eudiw.issuer.openid4vci.protocol;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Singular;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Builder
@AllArgsConstructor
public class CredentialOffer {

    @JsonProperty("credential_issuer")
    private String credentialIssuer;

    @JsonProperty("credential_configuration_ids")
    @Singular
    private List<String> credentialConfigurationIds;

    @JsonProperty("grants")
    private Grants grants;

}
