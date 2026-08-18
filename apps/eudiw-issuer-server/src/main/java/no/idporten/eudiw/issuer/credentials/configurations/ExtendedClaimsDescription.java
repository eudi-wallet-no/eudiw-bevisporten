package no.idporten.eudiw.issuer.credentials.configurations;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.issuer.credentials.types.ClaimDataType;
import no.idporten.eudiw.issuer.openid4vci.metadata.ClaimsDescription;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;

import java.util.List;
import java.util.Map;

/**
 * Extended claims description used internally in the credential issuer server.  Contains information about claim
 * type and validations.
 *
 * @param path claim path
 * @param type claim type
 * @param display display names
 * @param mandatory issuer must issue claim
 * @param validationRegex validation regex
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtendedClaimsDescription(
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        @JsonProperty("path") List<String> path,
        @JsonProperty("type")
        ClaimDataType type, // String, Number, Boolean, Binary, FullDate, DateTime, List, Map
        @JsonProperty("mime_type")
        String mimeType, // only for type = binary
        @JsonProperty("display")
        List<Display> display,
        @JsonProperty("mandatory")
        boolean mandatory,
        @JsonProperty("validation_regex")
        String validationRegex
)
{

    public static final String EMPTY_NAMESPACE = null;

    /**
     * Claim description, leaving out namespace (SD-JWT VC only). Do not use for sd-jwt of type Binary.
     *
     * @param name claim name
     * @param displayNames display names
     * @param mandatory issuer must issue claim
     * @param validationRegex validation regex
     */
    public ExtendedClaimsDescription(String namespace, String name, ClaimDataType type, Map<String, String> displayNames, boolean mandatory, String validationRegex) {
        this(convertPath(namespace, name), type, null, convert(displayNames), mandatory, validationRegex);
    }

    public ExtendedClaimsDescription(String namespace, String name, ClaimDataType type, String mimeType, Map<String, String> displayNames, boolean mandatory, String validationRegex) {
        this(convertPath(namespace, name), type, mimeType, convert(displayNames), mandatory, validationRegex);
    }

    public ExtendedClaimsDescription(String name, ClaimDataType type, String mimeType, Map<String, String> displayNames, boolean mandatory, String validationRegex) {
        this(null, name, type, mimeType,displayNames, mandatory, validationRegex);
    }

    private static List<Display> convert(Map<String, String> displayNames) {
        return displayNames.entrySet().stream().map(entry -> new Display(entry.getKey(), entry.getValue())).toList();
    }

    public static List<String> convertPath(String namespace, String path) {
        if (namespace == null || namespace.isEmpty()) {
            return List.of(path);
        }
        return List.of(namespace, path);
    }

    /**
     * This is a hack for the issuance API and other places in the issuer where shallow claim paths are assumed.
     *
     * @return last element in path
     */
    public String name() {
        return path.getLast();
    }

    /**
     * Convert to external model.
     */
    public ClaimsDescription toOpenID4VCIClaimsDescription() {
        return ClaimsDescription.builder()
                .path(path())
                .mandatory(mandatory)
                .display(display)
                .build();
    }
}
