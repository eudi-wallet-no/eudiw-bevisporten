package no.idporten.eudiw.issuer.claimssource;


import no.idporten.eudiw.issuer.claimssource.domain.Claim;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import org.springframework.util.StringUtils;

import java.util.List;

public interface ClaimsSource {

    /**
     * Gets Document metadata about claims provided by claims source.
     */
    DocumentMetadata getDocumentMetadata(CredentialMetadataContext credentialMetadataContext);

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
        return getProperties() != null && getProperties().getCredentialTypes().contains(credentialType);
    }

    /**
     * Issue claims (credentials).
     */
    List<Claim> issueClaims(CredentialIssueContext credentialIssueContext);

    /**
     * Indicates if this claims source has an authorative source of data.
     * If true, the claims source can be used in pull mode.
     */
    default boolean hasAuthorativeSource(){
        return StringUtils.hasText(getAuthorativeSourceName());
    }

    /**
     * Get the name of the authorative source of data.
     * @return null if no authorative source.
     */
    default String getAuthorativeSourceName(){
        return null;
    }

}
