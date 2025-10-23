package no.idporten.eudiw.issuer.claimssource;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.openid4vci.service.IssuanceTransactionId;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;

import java.util.Map;

/**
 * A pre-authorized claims source ensures that data is present and stored before a credential offer is created.
 * The claims source will either support a push mode (receive the data from the client application), or a pull mode
 * (it can retrieve the data itself).  Data is validated and stored by the claims source.
 */
public sealed interface PreAuthorizedClaimsSource extends ClaimsSource permits AbstractPreAuthorizedClaimsSource {

    /**
     * Pre-authorize credential issuance by validating and storing claims data.  Data can be provided by calling
     * system or fetched by the claims source if the pull() method is implemented.
     *
     * @param issuanceTransactionId
     * @param accessToken
     * @param pushedClaims
     */
    default void preAuthorize(IssuanceTransactionId issuanceTransactionId, JWT accessToken, Map<String, String> pushedClaims) {
        final Map<String, String> validatedClaims;
        if (!CollectionUtils.isEmpty(pushedClaims)) {
            validatedClaims = validate(push(issuanceTransactionId, accessToken, pushedClaims));
        } else {
            validatedClaims = validate(pull(issuanceTransactionId, accessToken));
        }
        store(issuanceTransactionId, validatedClaims);
    }

    /**
     * Pull claims data from authoritative source.  Disabled by default.
     */
    default Map<String, String> pull(IssuanceTransactionId issuanceTransactionId, JWT accessToken) {
        throw new IssuerServerException("invalid_request", "Credential configuration does not support pull of data", HttpStatus.BAD_REQUEST);
    }

    /**
     * Receive pushed data from authoritative source.  Disabled by default.
     */
    default Map<String, String> push(IssuanceTransactionId issuanceTransactionId, JWT accessToken, Map<String, String> claims) {
        throw new IssuerServerException("invalid_request", "Credential configuration does not support push of data", HttpStatus.BAD_REQUEST);
    }

    /**
     * Validate claims data.
     */
    Map<String, String> validate(Map<String, String> claims);

    /**
     * Store claims data in cache.
     */
    IssuanceTransactionId store(IssuanceTransactionId issuanceTransactionId, Map<String, String> claims);

}
