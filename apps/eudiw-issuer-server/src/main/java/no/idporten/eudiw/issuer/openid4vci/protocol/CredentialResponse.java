package no.idporten.eudiw.issuer.openid4vci.protocol;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Singular;

import java.util.List;

@Builder
@AllArgsConstructor
public class CredentialResponse {

    @JsonProperty("credentials")
    @Singular
    private List<Credential> credentials;

}
