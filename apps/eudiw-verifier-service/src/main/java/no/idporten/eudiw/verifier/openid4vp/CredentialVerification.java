package no.idporten.eudiw.verifier.openid4vp;

import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import no.idporten.eudiw.verifier.statuslist.StatuslistEntry;

import java.security.cert.X509Certificate;
import java.util.Map;

/**
 * Result of verifying a single presentation.
 *
 * @param format              credential format
 * @param credentialType      doctype for mdoc, vct for SD-JWT VC
 * @param claims              credential claims
 * @param issuerCertificate   the certificate the issuer signature was verified with, null if it could not be determined
 * @param statuslistEntry     null if the credential has no status
 * @param credentialStatus    issuer signature, integrity and validity period of the credential
 * @param holderBindingStatus proof of possession of the holder key, NOT_APPLICABLE when not verified
 */
public record CredentialVerification(
        CredentialFormat format,
        String credentialType,
        Map<String, Object> claims,
        X509Certificate issuerCertificate,
        StatuslistEntry statuslistEntry,
        ValidationStatus credentialStatus,
        ValidationStatus holderBindingStatus) {
}
