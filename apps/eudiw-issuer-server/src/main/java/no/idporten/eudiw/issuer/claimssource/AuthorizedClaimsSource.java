package no.idporten.eudiw.issuer.claimssource;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.domain.Claim;
import org.springframework.http.HttpStatus;

import java.text.ParseException;
import java.util.List;

/**
 * A pre-authorized claims source ensures that data is present and stored before a credential offer is created.
 * The claims source will either support a push mode (receive the data from the client application), or a pull mode
 * (it can retrieve the data itself).  Data is validated and stored by the claims source.
 */
public sealed interface AuthorizedClaimsSource extends ClaimsSource permits AbstractAuthorizedClaimsSource {


    @Override
    default List<Claim> retrieveClaims(JWT accessToken){
        String personIdentifier;
        try {
            personIdentifier = accessToken.getJWTClaimsSet().getSubject();
        } catch (ParseException e) {
            throw new IssuerServerException("invalid_token", "Failed to extract fnr/dnr from access token", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
        return pull(accessToken); // TODO pass personIdentifier???
    }

    /**
     * Pull claims data from authoritative source.  Disabled by default.
     */
    default List<Claim> pull(JWT accessToken) {
        throw new IssuerServerException("invalid_request", "Credential configuration does not support pull of data", HttpStatus.BAD_REQUEST);
    }

}
