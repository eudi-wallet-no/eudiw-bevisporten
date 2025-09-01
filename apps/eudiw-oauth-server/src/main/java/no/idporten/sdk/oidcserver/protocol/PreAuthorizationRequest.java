package no.idporten.sdk.oidcserver.protocol;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PreAuthorizationRequest {

    @JsonProperty("sub")
    private String sub;
    @JsonProperty("aud")
    private String aud;
    @JsonProperty("scope")
    private List<String> scope;
    @JsonProperty("tx_code_challenge")
    private String txCodeChallenge;
    @JsonProperty("tx_id")
    private String txId;
    @JsonProperty("authorization_token_lifetime")
    private int authorizationLifetimeSeconds = 120;

}
