package no.idporten.eudiw.issuer.claimssource;


import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.claimssource.domain.Claim;
import no.idporten.eudiw.issuer.config.ClaimsSourceProperties;

import java.util.List;
import java.util.Objects;

public interface ClaimsSource {

    /**
     * Initializes claims source with properties.
     */
    void init(ClaimsSourceProperties properties);

    /**
     * Gets properties for this claims source.
     */
    ClaimsSourceProperties getProperties();

    /**
     * Gets metadata about claims provided by claims source.
     */
    ClaimsSourceMetadata getMetadata();

    /**
     * Check if this claims source supports doctype
     */
    default boolean supports(String doctype) {
        return Objects.equals(doctype, getProperties().getDoctype());
    }

    /**
     * Retrieves claims.
     */
    List<Claim> retrieveClaims(JWT accessToken);

}
