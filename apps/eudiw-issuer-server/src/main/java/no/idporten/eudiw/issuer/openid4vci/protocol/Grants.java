package no.idporten.eudiw.issuer.openid4vci.protocol;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@AllArgsConstructor
public class Grants {

    @JsonProperty("urn:ietf:params:oauth:grant-type:pre-authorized_code")
    private PreAuthorizedCodeGrant preAuthorizedCodeGrant;

    @JsonProperty("authorization_code")
    private AuthorizedCodeGrant authorizedCodeGrant;

}

