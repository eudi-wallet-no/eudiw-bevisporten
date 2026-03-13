package no.idporten.eudiw.issuer.claimssource;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialMetadata;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;

import java.time.Duration;

/**
 * Context for pre-authorized credential issuance.
 *
 * @param issuanceTransactionId issuance transaction id
 * @param credentialConfigurationId the credential configuration id
 * @param accessToken access token for issuance transaction
 * @param authorizationLifetime lifetime for pre-authorization
 */
public record PreAuthorizedIssuanceContext(
        IssuanceTransactionId issuanceTransactionId,
        CredentialIssuerTenant credentialIssuerTenant,
        ExtendedCredentialMetadata credentialMetadata,
        String credentialConfigurationId,
        JWT accessToken,
        Duration authorizationLifetime) {

    public PreAuthorizedIssuanceContext(IssuanceTransactionId issuanceTransactionId, CredentialIssuerTenant credentialIssuerTenant, JWT accessToken) {
        this(issuanceTransactionId, credentialIssuerTenant, null, null, accessToken, null);
    }

}