package no.idporten.eudiw.issuer.claimssource;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;

import java.time.Duration;

/**
 * Context for pre-authorized credential issuance.
 *
 * @param credentialIssuerTenant
 * @param credentialConfiguration
 * @param issuanceTransactionId issuance transaction id
 * @param accessToken access token for issuance transaction
 * @param authorizationLifetime lifetime for pre-authorization
 */
public record PreAuthorizedIssuanceContext(
        CredentialIssuerTenant credentialIssuerTenant,
        ExtendedCredentialConfiguration credentialConfiguration,
        IssuanceTransactionId issuanceTransactionId,
        JWT accessToken,
        Duration authorizationLifetime) {

    public PreAuthorizedIssuanceContext(CredentialIssuerTenant credentialIssuerTenant, ExtendedCredentialConfiguration credentialConfiguration, IssuanceTransactionId issuanceTransactionId, JWT accessToken) {
        this(credentialIssuerTenant, credentialConfiguration, issuanceTransactionId, accessToken, Duration.ofMinutes(10));
    }

}