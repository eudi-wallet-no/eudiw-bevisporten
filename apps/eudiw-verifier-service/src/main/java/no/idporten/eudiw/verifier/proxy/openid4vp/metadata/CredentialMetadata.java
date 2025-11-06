package no.idporten.eudiw.verifier.proxy.openid4vp.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CredentialMetadata {

    @JsonProperty("display")
    private List<Display> display;

    @JsonProperty("claims")
    private List<ClaimsDescription> claimsDescriptions;
}
