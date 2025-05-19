package no.idporten.eudiw.issuer.openid4vci.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * Claims description for issuer metadata - https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#claims-description-issuer-metadata
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClaimsDescription {

    @Singular("path")
    @JsonProperty("path")
    private List<String> path;

    @Builder.Default
    @JsonProperty("mandatory")
    private boolean mandatory = true;

    @Singular("display")
    @JsonProperty("display")
    private List<Display> display;

}
