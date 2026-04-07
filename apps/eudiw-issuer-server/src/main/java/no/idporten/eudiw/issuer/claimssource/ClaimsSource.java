package no.idporten.eudiw.issuer.claimssource;


import no.idporten.eudiw.issuer.credentials.types.Claim;

import java.util.List;

public sealed interface ClaimsSource permits AuthorizedClaimsSource, PreAuthorizedClaimsSource{

    /**
     * Issue claims (credentials).
     */
    List<Claim> issueClaims(CredentialIssueContext credentialIssueContext);

}
