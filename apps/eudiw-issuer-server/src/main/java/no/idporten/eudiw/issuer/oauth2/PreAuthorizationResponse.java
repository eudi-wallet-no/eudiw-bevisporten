package no.idporten.eudiw.issuer.oauth2;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PreAuthorizationResponse {

    @JsonProperty("pre-authorized_code")
    private String preAuthorizedCode;
    @JsonProperty("expires_in")
    private long expiresInSeconds;

}
