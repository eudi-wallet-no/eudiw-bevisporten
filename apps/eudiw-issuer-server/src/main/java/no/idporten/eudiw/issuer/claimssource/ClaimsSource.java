package no.idporten.eudiw.issuer.claimssource;


import no.idporten.eudiw.issuer.credentials.types.Claim;
import no.idporten.eudiw.issuer.credentials.types.DocumentMetadata;

import java.util.List;

public interface ClaimsSource {

    /**
     * Gets Document metadata about claims provided by claims source.
     */
    DocumentMetadata getDocumentMetadata(CredentialMetadataContext credentialMetadataContext);

    /**
     * Issue claims (credentials).
     */
    List<Claim> issueClaims(CredentialIssueContext credentialIssueContext);

    /**
     * Get the name of the authorative source of data.
     * @return null if no authorative source.
     */
    default String getAuthorativeSourceName(){
        return null;
    }

}
