package no.idporten.eudiw.issuer.openid4vci.protocol;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import no.idporten.eudiw.issuer.claimssource.Claim;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StartIssuanceRequest {

    @JsonProperty("credential_issuer")
    private String credentialIssuer;

    @JsonProperty("credential_configuration_id")
    private String credentialConfigurationId;

    @JsonProperty("claims")
    private List<Claim> claims;

}
