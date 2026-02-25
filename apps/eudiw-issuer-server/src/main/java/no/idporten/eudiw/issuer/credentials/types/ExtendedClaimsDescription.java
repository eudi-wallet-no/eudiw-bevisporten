package no.idporten.eudiw.issuer.credentials.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.issuer.openid4vci.metadata.ClaimsDescription;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Extended claims description used internally in the credential issuer server.  Contains information about claim
 * type and validations.
 *
 * @param namespace namespace (mdoc only, first in path)
 * @param name claim name (last in path)
 * @param type claim type
 * @param display display names
 * @param mandatory issuer must issue claim
 * @param validationRegex validation regex
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtendedClaimsDescription(
        @JsonProperty("namespace")
        String namespace, // only for mdoc, first in path - shallow credentials
        @JsonProperty("name")
        String name, // last in path mdoc, complete path SD-JWT VC - shallow credentials
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
    public ExtendedClaimsDescription(String name, ClaimDataType type, Map<String, String> displayNames, boolean mandatory, String validationRegex) {
        this(null, name, type, null, convert(displayNames), mandatory, validationRegex);
    }
    /**
     * Claim description, leaving out namespace (SD-JWT VC only). Do not use for sd-jwt of type Binary.
     *
     * @param name claim name
     * @param displayNames display names
     * @param mandatory issuer must issue claim
     * @param validationRegex validation regex
     */
    public ExtendedClaimsDescription(String namespace, String name, ClaimDataType type, Map<String, String> displayNames, boolean mandatory, String validationRegex) {
        this(namespace, name, type, null, convert(displayNames), mandatory, validationRegex);
    }

    /**
     * Claim description, using string as type and leaving out namespace (SD-JWT VC only).
     *
     * @param name claim name
     * @param displayNames display names
     * @param mandatory issuer must issue claim
     * @param validationRegex validation regex
     */
    public ExtendedClaimsDescription(String name, Map<String, String> displayNames, boolean mandatory, String validationRegex) {
        this(null, name, ClaimDataType.STRING, null, convert(displayNames), mandatory, validationRegex);
    }

    public ExtendedClaimsDescription(String namespace, String name, ClaimDataType type, String mimeType, Map<String, String> displayNames, boolean mandatory, String validationRegex) {
        this(namespace, name, type, mimeType, convert(displayNames), mandatory, validationRegex);
    }

    private static List<Display> convert(Map<String, String> displayNames) {
        return displayNames.entrySet().stream().map(entry -> new Display(entry.getKey(), entry.getValue())).toList();
    }

    public List<String> path() {
        if (namespace != null) {
            return List.of(namespace, name);
        }
        return Collections.singletonList(name);
    }

    /**
     * Convert to external model.
     */
    public ClaimsDescription toClaimsDescription() {
        return ClaimsDescription.builder()
                .path(path())
                .mandatory(mandatory)
                .display(display)
                .build();
    }
}
