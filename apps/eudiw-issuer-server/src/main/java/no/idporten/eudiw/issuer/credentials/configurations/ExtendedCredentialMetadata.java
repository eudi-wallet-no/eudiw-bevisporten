package no.idporten.eudiw.issuer.credentials.configurations;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import no.idporten.eudiw.issuer.openid4vci.metadata.CredentialMetadata;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;

import java.util.List;

/**
 * Extended credential metadata used internally in the credential issuer server.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtendedCredentialMetadata(
    List<Display> display,
    List<ExtendedClaimsDescription> claims
) {

    public ExtendedClaimsDescription findClaimMetadata(String name) {
        return claims().stream().filter(claim -> claim.name().equals(name)).findFirst().orElse(null);
    }

    /**
     * Convert to external model.
     */
    public CredentialMetadata toOpenID4VCICredentialMetadata() {
        return CredentialMetadata.builder()
                .display(display())
                .claims(claims().stream().map(ExtendedClaimsDescription::toOpenID4VCIClaimsDescription).toList())
                .build();
    }

}
