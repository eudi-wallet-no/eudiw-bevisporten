package no.idporten.eudiw.issuer.claimssource;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import no.idporten.eudiw.issuer.openid4vci.metadata.ClaimsDescription;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimsSourceMetadata {

    @Singular("display")
    @JsonProperty("display")
    private List<Display> display;

    @Singular("claims")
    @JsonProperty("claims")
    private List<ClaimsDescription> claims;

}
