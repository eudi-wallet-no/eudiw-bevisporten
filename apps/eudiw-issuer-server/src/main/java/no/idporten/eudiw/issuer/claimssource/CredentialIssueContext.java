package no.idporten.eudiw.issuer.claimssource;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialMetadata;

/**
 * Context for credential issuance.
 *
 * @param accessToken validated access token
 * @param credentialIssuerTenant credential issuer tenant issuing the credential
 * @param credentialMetadata credential metadata for the credential being issued
 */
public record CredentialIssueContext(
        JWT accessToken,
        CredentialIssuerTenant credentialIssuerTenant,
        ExtendedCredentialMetadata credentialMetadata) {
}