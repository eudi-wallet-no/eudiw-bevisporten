package no.idporten.eudiw.issuer.claimssource;


import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.claimssource.domain.Claim;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;

import java.util.List;

public interface ClaimsSource {

    /**
     * Gets Document metadata about claims provided by claims source.
     */
    DocumentMetadata getDocumentMetadata();

    /**
     * Initializes claims source with properties.
     */
    void init(ClaimsSourceProperties properties);

    /**
     * Gets properties for this claims source.
     */
    ClaimsSourceProperties getProperties();

    /**
     * Check if this claims source supports credential type
     */
    default boolean supports(String credentialType) {
        return getProperties().getCredentialTypes().contains(credentialType);
    }

    /**
     * Issue claims (credentials).
     */
    List<Claim> issueClaims(JWT accessToken);

}
