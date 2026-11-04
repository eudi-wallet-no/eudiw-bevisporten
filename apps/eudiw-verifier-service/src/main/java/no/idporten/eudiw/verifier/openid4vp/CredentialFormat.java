package no.idporten.eudiw.verifier.openid4vp;

import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationType;
import no.idporten.eudiw.verifier.statuslist.StatusListTokenFormat;

/**
 * The credential formats supported in DCQL queries, see OpenID4VP 1.0 appendix B.
 */
public enum CredentialFormat {

    DC_SD_JWT("dc+sd-jwt", ValidationType.SDJWT, StatusListTokenFormat.JWT),
    MSO_MDOC("mso_mdoc", ValidationType.MDOC, StatusListTokenFormat.CWT);

    private final String dcqlFormat;
    private final ValidationType validationType;
    private final StatusListTokenFormat statusListTokenFormat;

    CredentialFormat(String dcqlFormat, ValidationType validationType, StatusListTokenFormat statusListTokenFormat) {
        this.dcqlFormat = dcqlFormat;
        this.validationType = validationType;
        this.statusListTokenFormat = statusListTokenFormat;
    }

    public static CredentialFormat fromDcql(String format) {
        for (CredentialFormat credentialFormat : values()) {
            if (credentialFormat.dcqlFormat.equals(format)) {
                return credentialFormat;
            }
        }
        throw new VerificationException("invalid_request", "Unsupported credential format: " + format);
    }

    public ValidationType validationType() {
        return validationType;
    }

    public StatusListTokenFormat statusListTokenFormat() {
        return statusListTokenFormat;
    }
}
