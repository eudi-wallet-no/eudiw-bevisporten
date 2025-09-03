package no.idporten.eudiw.issuer.openid4vci.protocol;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public class Grants {

    @JsonProperty("urn:ietf:params:oauth:grant-type:pre-authorized_code")
    private PreAuthorizedCodeGrant preAuthorizedCodeGrant;

}

