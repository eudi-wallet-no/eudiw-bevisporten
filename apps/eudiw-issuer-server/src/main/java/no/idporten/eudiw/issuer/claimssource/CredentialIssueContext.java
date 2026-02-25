package no.idporten.eudiw.issuer.claimssource;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.credentials.types.ExtendedCredentialMetadata;

/**
 * Context for credential issuance.
 *
 * @param accessToken               validated access token
 * @param credentialMetadata credential metadata for the credential being issued
 */
public record CredentialIssueContext(
        JWT accessToken,
        ExtendedCredentialMetadata credentialMetadata) {
}