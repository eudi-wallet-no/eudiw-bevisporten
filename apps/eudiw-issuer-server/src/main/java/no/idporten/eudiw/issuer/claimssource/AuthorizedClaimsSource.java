package no.idporten.eudiw.issuer.claimssource;

import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.credentials.types.Claim;
import org.springframework.http.HttpStatus;

import java.text.ParseException;
import java.util.List;

/**
 * An authorized claims source ensures that it can pull data from an authoritative source based on the subject (fnr/dnr)
 * present in the access token.
 */
public sealed interface AuthorizedClaimsSource extends ClaimsSource permits AbstractAuthorizedClaimsSource {


    @Override
    default List<Claim> issueClaims(CredentialIssueContext credentialIssueContext){
        String personIdentifier;
        try {
            personIdentifier = credentialIssueContext.accessToken().getJWTClaimsSet().getSubject();
        } catch (ParseException e) {
            throw new IssuerServerException("invalid_token", "Failed to extract fnr/dnr from access token", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
        if(personIdentifier == null || personIdentifier.isBlank()){
            throw new IssuerServerException("invalid_token", "Access token is missing subject (fnr/dnr)", HttpStatus.BAD_REQUEST);
        }
        return pull(personIdentifier);
    }

    /**
     * Pull claims data from authoritative source.  Disabled by default.
     */
    default List<Claim> pull(String personIdentifier) {
        throw new IssuerServerException("invalid_request", "Credential configuration does not support pull of data", HttpStatus.BAD_REQUEST);
    }

}
