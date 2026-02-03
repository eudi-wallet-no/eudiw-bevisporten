package no.idporten.eudiw.issuer.claimssource;

import no.idporten.eudiw.issuer.credentials.formats.CredentialFormat;

/**
 * Credential metadata context for claims source from issuer.  Helps claims sources adapt to different credential
 * configurations.
 *
 * @param credentialConfigurationId the credential configuration for this issuance
 * @param credentialType the credential type being issued
 * @param format the format being issued
 */
public record CredentialMetadataContext(
        String credentialConfigurationId,
        String credentialType,
        CredentialFormat format
){
}
