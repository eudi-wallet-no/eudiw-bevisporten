package no.idporten.eudiw.issuer.claimssource;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.exception.ErrorCode;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;

/**
 * Context for credential issuance.
 *
 * @param accessToken validated access token
 * @param credentialIssuerTenant credential issuer tenant issuing the credential
 * @param credentialConfiguration credential configuration for the credential being issued
 */
public record CredentialIssueContext(
        JWT accessToken,
        CredentialIssuerTenant credentialIssuerTenant,
        ExtendedCredentialConfiguration credentialConfiguration) {

    /**
     * Extract person identifier from access token subject.
     */
    public String personIdentifier() {
        try {
            String personIdentifier = accessToken.getJWTClaimsSet().getSubject();
            if(personIdentifier == null || personIdentifier.isBlank()){
                throw new IssuerServerException(ErrorCode.INVALID_TOKEN, "Access token is missing subject");
            }
            return  personIdentifier;
        } catch (Exception e) {
            throw new IssuerServerException(ErrorCode.INVALID_TOKEN, "Failed to extract subject from access token", e);
        }
    }

    /**
     * Extract transaction id from access token.  Only applicable for pre-authorized credential issuance.
     */
    public IssuanceTransactionId transactionId() {
        try {
            String transactionId = accessToken().getJWTClaimsSet().getStringClaim("tx_id");
            if(transactionId == null || transactionId.isBlank()){
                return null;
            }
            return new IssuanceTransactionId(transactionId);
        } catch (Exception e) {
            throw new IssuerServerException(ErrorCode.INVALID_TOKEN, "Missing claim [%s] in access token".formatted("tx_id"));
        }
    }

}