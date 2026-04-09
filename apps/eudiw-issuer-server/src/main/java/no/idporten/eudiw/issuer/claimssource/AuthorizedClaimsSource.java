package no.idporten.eudiw.issuer.claimssource;

import no.idporten.eudiw.issuer.claimssource.exception.CredentialRequestDeniedException;

/**
 * An authorized claims source ensures that it can pull data from an authoritative source based on the subject (fnr/dnr)
 * present in the access token.
 */
non-sealed interface AuthorizedClaimsSource extends ClaimsSource {

    /**
     * Pull claims data from authoritative source.  Disabled by default.
     */
    default CredentialData pull(CredentialIssueContext credentialIssueContext) {
        throw new CredentialRequestDeniedException(credentialIssueContext.credentialConfiguration().getCredentialConfigurationId(), "Credential configuration does not support pull of data");
    }

}
