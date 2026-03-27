package no.idporten.eudiw.issuer.claimssource;

import no.idporten.eudiw.issuer.IssuerServerException;
import org.springframework.http.HttpStatus;

/**
 * An authorized claims source ensures that it can pull data from an authoritative source based on the subject (fnr/dnr)
 * present in the access token.
 */
non-sealed interface AuthorizedClaimsSource extends ClaimsSource {

    /**
     * Pull claims data from authoritative source.  Disabled by default.
     */
    default CredentialData pull(CredentialIssueContext credentialIssueContext) {
        throw new IssuerServerException("invalid_request", "Credential configuration does not support pull of data", HttpStatus.BAD_REQUEST);
    }

}
