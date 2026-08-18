package no.idporten.eudiw.verifier.api.openid4vp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.net.URI;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class WalletCallback {

    @JsonProperty("redirect_uri")
    private URI redirectUri;

}
