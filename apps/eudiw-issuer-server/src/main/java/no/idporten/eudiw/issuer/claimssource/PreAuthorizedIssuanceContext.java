package no.idporten.eudiw.issuer.claimssource;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.springframework.http.HttpStatus;

import java.time.Duration;

/**
 * Context for pre-authorized credential issuance.
 *
 * @param credentialIssuerTenant
 * @param credentialConfiguration
 * @param transactionId issuance transaction id
 * @param accessToken access token for issuance transaction
 * @param authorizationLifetime lifetime for pre-authorization
 */
public record PreAuthorizedIssuanceContext(
        CredentialIssuerTenant credentialIssuerTenant,
        ExtendedCredentialConfiguration credentialConfiguration,
        IssuanceTransactionId transactionId,
        JWT accessToken,
        Duration authorizationLifetime) {

    public PreAuthorizedIssuanceContext(CredentialIssuerTenant credentialIssuerTenant, ExtendedCredentialConfiguration credentialConfiguration, IssuanceTransactionId issuanceTransactionId, JWT accessToken) {
        this(credentialIssuerTenant, credentialConfiguration, issuanceTransactionId, accessToken, Duration.ofMinutes(10));
    }

    /**
     * Extract person identifier from access token pid claim.
     */
    public String personIdentifier() {
        try {
            String personIdentifier = accessToken.getJWTClaimsSet().getStringClaim("pid");
            if(personIdentifier == null || personIdentifier.isBlank()){
                throw new IssuerServerException("invalid_token", "Access token is missing pid claim", HttpStatus.BAD_REQUEST);
            }
            return  personIdentifier;
        } catch (Exception e) {
            throw new IssuerServerException("invalid_token", "Failed to extract subject from access token", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

}