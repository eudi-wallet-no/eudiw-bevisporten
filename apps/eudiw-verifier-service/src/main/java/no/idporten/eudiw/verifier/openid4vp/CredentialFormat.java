package no.idporten.eudiw.verifier.openid4vp;

import no.idporten.eudiw.verifier.ErrorCode;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationType;

/**
 * The credential formats supported in DCQL queries, see OpenID4VP 1.0 appendix B.
 */
public enum CredentialFormat {

    DC_SD_JWT("dc+sd-jwt", ValidationType.SDJWT),
    MSO_MDOC("mso_mdoc", ValidationType.MDOC);

    private final String format;
    private final ValidationType validationType;

    CredentialFormat(String format, ValidationType validationType) {
        this.format = format;
        this.validationType = validationType;
    }

    public static CredentialFormat fromString(String format) {
        for (CredentialFormat credentialFormat : values()) {
            if (credentialFormat.format.equals(format)) {
                return credentialFormat;
            }
        }
        throw new VerificationException(ErrorCode.VP_FORMATS_NOT_SUPPORTED, "Unsupported credential format: " + format);
    }

    public ValidationType validationType() {
        return validationType;
    }
}
