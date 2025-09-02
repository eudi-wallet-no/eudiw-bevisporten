package no.idporten.eudiw.issuer.oauth2;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PreAuthorizationRequest {

    @JsonProperty("sub")
    private String sub;
    @JsonProperty("aud")
    private String aud;
    @JsonProperty("scope")
    @Singular
    private List<String> scopes;
    @JsonProperty("tx_code_challenge")
    private String txCodeChallenge;
    @JsonProperty("tx_id")
    private String txId;
    @JsonProperty("authorization_token_lifetime")
    private int authorizationLifetimeSeconds = 120;

}
