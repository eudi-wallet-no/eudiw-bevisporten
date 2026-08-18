package no.idporten.eudiw.issuer.credentials.formats;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Credential formats.  The format() method gives the spec compliant value from
 */
public enum CredentialFormat {

    /**
     * Credential format identifier for ISO/IEC 18013 Mobile Documents: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-format-identifier-4
     */
    @JsonProperty("mso_mdoc")
    MSO_MDOC("mso_mdoc"),
    /**
     * Credential format identifier for IETF SD-JWT VC: https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html#name-format-identifier-5
     */
    @JsonProperty("dc+sd-jwt")
    SD_JWT_VC("dc+sd-jwt");


    CredentialFormat(String formatIdentifier) {
        this.formatIdentifier = formatIdentifier;
    }

    private final String formatIdentifier;

    public String formatIdentifier() {
        return formatIdentifier;
    }

    public static CredentialFormat fromString(String value) {
        for (CredentialFormat format : values()) {
            if (format.formatIdentifier.equals(value)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Unknown CredentialFormat: " + value);
    }
}
