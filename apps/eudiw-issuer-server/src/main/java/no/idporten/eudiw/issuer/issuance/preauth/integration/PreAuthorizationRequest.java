package no.idporten.eudiw.issuer.issuance.preauth.integration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
    @JsonProperty("authorization_lifetime")
    private long authorizationLifetimeSeconds;

}
