package no.idporten.eudiw.issuer.claimssource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import no.idporten.eudiw.issuer.openid4vci.metadata.ClaimsDescription;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;

import java.util.Arrays;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimsSourceMetadata {

    @Singular("display")
    @JsonProperty("display")
    private List<Display> displays;

    @Singular("claim")
    @JsonProperty("claims")
    private List<ClaimsDescription> claims;

    @JsonIgnore
    public ClaimsDescription findClaimsDescription(String... path) {
        return claims.stream()
                .filter(claimsDescription -> claimsDescription.getPath().equals(Arrays.stream(path).toList()))
                .findFirst()
                .orElse(null);
    }

}
