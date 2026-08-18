package no.idporten.eudiw.issuer.context;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;

/**
 * Context for credential revocation for push-based credentials.
 *
 * @param accessToken access token with necessary scopes the credential type to be revoked
 * @param credentialIssuerTenant credential issuer tenant
 * @param credentialConfiguration credential configuration for the credential being revoked
 * @param transactionId issuance transaction id associated with the credential issuance process
 * */
public record CredentialRevokeContext(
        JWT accessToken,
        CredentialIssuerTenant credentialIssuerTenant,
        ExtendedCredentialConfiguration credentialConfiguration,
        IssuanceTransactionId transactionId) {
}
