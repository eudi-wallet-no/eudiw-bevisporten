package no.idporten.eudiw.issuer.openid4vci.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * Credential metadata for a credential configuration.
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialMetadata {

    @JsonProperty("display")
    private List<Display> display;

    @Singular("claim")
    @JsonProperty("claims")
    private List<ClaimsDescription> claims;

}
