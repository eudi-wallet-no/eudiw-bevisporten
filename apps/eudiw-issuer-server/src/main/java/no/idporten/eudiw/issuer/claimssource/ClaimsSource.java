package no.idporten.eudiw.issuer.claimssource;


import no.idporten.eudiw.issuer.credentials.types.Claim;

import java.util.List;

public interface ClaimsSource {

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
