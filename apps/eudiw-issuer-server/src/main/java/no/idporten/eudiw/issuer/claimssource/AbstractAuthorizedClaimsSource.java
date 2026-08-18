package no.idporten.eudiw.issuer.claimssource;

import no.idporten.eudiw.issuer.credentials.types.Claim;

import java.util.List;

public abstract class AbstractAuthorizedClaimsSource extends AbstractClaimsSource implements AuthorizedClaimsSource {

    @Override
    public List<Claim> issueClaims(CredentialIssueContext credentialIssueContext) {
        CredentialData credentialData = pull(credentialIssueContext);
        credentialData = validate(credentialIssueContext.credentialConfiguration().getExtendedCredentialMetadata(), credentialData);
        return convertCredentialDataToClaims(credentialIssueContext, credentialData.claims());
    }

}
