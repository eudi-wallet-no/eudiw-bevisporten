package no.idporten.eudiw.issuer.claimssource;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.issuance.CredentialIssuanceType;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.eudiw.issuer.oauth2.InvalidAccessTokenException;
import no.idporten.eudiw.issuer.oauth2.WalletInstanceAttestation;
import no.idporten.eudiw.issuer.openid4vci.proofs.BindingKeys;

/**
 * Context for credential issuance.
 *
 * @param accessToken validated access token
 * @param credentialIssuerTenant credential issuer tenant issuing the credential
 * @param credentialConfiguration credential configuration for the credential being issued
 * @param issuanceTransactionId issuance transaction id, resolved by the caller before building the context
 * @param issuanceType the grant type flow in use, determined from the access token
 * @param bindingKeys binding keys from the validated proofs in the credential request
 * @param walletInstanceAttestation wallet instance attestation from the access_token, or null if not present
 */
public record CredentialIssueContext(
        JWT accessToken,
        CredentialIssuerTenant credentialIssuerTenant,
        ExtendedCredentialConfiguration credentialConfiguration,
        IssuanceTransactionId issuanceTransactionId,
        CredentialIssuanceType issuanceType,
        BindingKeys bindingKeys,
        WalletInstanceAttestation walletInstanceAttestation) {

    /**
     * Creates context with wallet instance attestation extracted from the access_token.
     */
    public CredentialIssueContext(
            JWT accessToken,
            CredentialIssuerTenant credentialIssuerTenant,
            ExtendedCredentialConfiguration credentialConfiguration,
            IssuanceTransactionId issuanceTransactionId,
            CredentialIssuanceType issuanceType,
            BindingKeys bindingKeys) {
        this(accessToken, credentialIssuerTenant, credentialConfiguration, issuanceTransactionId, issuanceType, bindingKeys,
                WalletInstanceAttestation.fromAccessToken(accessToken));
    }

    /**
     * Extract person identifier from access token subject.
     */
    public String personIdentifier() {
        try {
            String personIdentifier = accessToken.getJWTClaimsSet().getSubject();
            if(personIdentifier == null || personIdentifier.isBlank()){
                throw new InvalidAccessTokenException("Access token is missing subject");
            }
            return  personIdentifier;
        } catch (Exception e) {
            throw new InvalidAccessTokenException("Failed to extract subject from access token", e);
        }
    }

    public IssuanceTransactionId transactionId() {
        return issuanceTransactionId;
    }

}
