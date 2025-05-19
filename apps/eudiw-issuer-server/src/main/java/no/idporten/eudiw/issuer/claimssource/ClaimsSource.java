package no.idporten.eudiw.issuer.claimssource;


import no.idporten.eudiw.issuer.config.ClaimsSourceProperties;
import no.idporten.eudiw.issuer.openid4vci.metadata.ClaimsDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class ClaimsSource {

    private ClaimsSourceProperties properties;

    public void init(ClaimsSourceProperties properties) {
        this.properties = properties;
    }

    public boolean isEnabled() {
        return true;
    }

    /**
     * Gets metadata about the claims this claims source can provide.
     */
    public ClaimsSourceMetadata getMetadata() {
        return ClaimsSourceMetadata.builder()
                .display(properties.getDisplay())
                .claims(properties.getClaims())
                .build();
    }

    /**
     * Check if this claims supports doctype
     */
    public boolean supports(String doctype) {
        return Objects.equals(doctype, properties.getDoctype());
    }

    /**
     * Retrieves claims.
     */
    public List<Claim> retrieveClaims() {
        // mock response data
        List<Claim> claims = new ArrayList<>();
        for (ClaimsDescription claimsDescription : properties.getClaims()) {
            if (claimsDescription.isMandatory()) {
                for (String path : claimsDescription.getPath()) {
                    claims.add(Claim.builder().path(path).value("foo" + new Random().nextInt()).build());
                }
            }
        }
        return claims;
    }

}
