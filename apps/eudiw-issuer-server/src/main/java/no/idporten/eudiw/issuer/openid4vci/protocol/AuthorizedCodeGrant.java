package no.idporten.eudiw.issuer.openid4vci.protocol;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Getter
@Builder
@AllArgsConstructor
public class AuthorizedCodeGrant {

    @JsonProperty("issuer_state")
    private String issuerState;

    @JsonProperty("authorization_server")
    private String authorizationServer;

}
