package no.idporten.eudiw.issuer.claimssource;

import com.nimbusds.jwt.JWT;

/**
 * Context for credential issuance.
 *
 * @param accessToken               validated access token
 * @param credentialConfigurationId
 */
public record CredentialIssueContext(
        JWT accessToken,
        String credentialConfigurationId) {
}