package no.idporten.eudiw.issuer.credentials.types;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import no.idporten.eudiw.issuer.openid4vci.metadata.CredentialMetadata;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;

import java.util.Arrays;
import java.util.List;

/**
 * Extended credential metadata used internally in the credential issuer server.
 *
 * @param display
 * @param claims
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtendedCredentialMetadata(
    List<Display> display,
    List<ExtendedClaimsDescription> claims
) {

    public ExtendedClaimsDescription findClaimMetadata(String name) {
        return claims().stream().filter(claim -> claim.name().equals(name)).findFirst().orElse(null);
    }

    public ExtendedClaimsDescription findClaimMetadata(String... path) {
        return claims().stream().filter(claim -> claim.path().equals(Arrays.stream(path).toList())).findFirst().orElse(null);
    }

    /**
     * Convert to external model.
     */
    public CredentialMetadata toCredentialMetadata() {
        return CredentialMetadata.builder()
                .display(display())
                .claims(claims().stream().map(ExtendedClaimsDescription::toClaimsDescription).toList())
                .build();
    }

}
