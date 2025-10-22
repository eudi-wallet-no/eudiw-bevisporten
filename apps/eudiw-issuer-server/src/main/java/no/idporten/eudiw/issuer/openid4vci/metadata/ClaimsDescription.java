package no.idporten.eudiw.issuer.openid4vci.metadata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import no.idporten.eudiw.issuer.openid4vci.CredentialFormat;

import java.util.ArrayList;
import java.util.List;

/**
 * Claims description for issuer metadata - https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#claims-description-issuer-metadata
 */
@Data
@With
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClaimsDescription {

    @JsonProperty("path")
    @Singular("path")
    private List<String> path;

    @Builder.Default
    @JsonProperty("mandatory")
    private boolean mandatory = true;

    @Singular("display")
    @JsonProperty("display")
    private List<Display> displays;

    @JsonIgnore
    public Display findDisplay(String locale) {
        return displays.stream()
                .filter(display -> display.getLocale().equals(locale))
                .findFirst()
                .orElse(displays.getFirst());
    }

    public ClaimsDescription forFormat(CredentialFormat credentialFormat, String credentialType) {
        if (CredentialFormat.SD_JWT_VC.equals(credentialFormat)) {
            return this;
        }
        List<String> mdocPath = new ArrayList<>();
        mdocPath.add(credentialType);
        mdocPath.addAll(this.path);
        return this.withPath(mdocPath);
    }

}
