package no.idporten.eudiw.issuer.openid4vci;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public class Credential {

    @JsonProperty("credential")
    private String credential;

}
