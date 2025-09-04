package no.idporten.eudiw.issuer.openid4vci.protocol;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private List<Claim> claims = new ArrayList<>();

    public Map<String, String> getClaimsMap() {
        return claims.stream().collect(Collectors.toMap(Claim::getName, Claim::getValue));
    }

}
