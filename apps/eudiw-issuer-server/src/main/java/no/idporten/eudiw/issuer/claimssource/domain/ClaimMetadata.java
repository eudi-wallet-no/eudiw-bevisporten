package no.idporten.eudiw.issuer.claimssource.domain;

import no.idporten.eudiw.issuer.claimssource.ClaimDataTypes;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Claim description.
 *
 * @param namespace namespace (mdoc only, first in path)
 * @param name claim name (last in path)
 * @param type claim type
 * @param displayNames display names
 * @param mandatory issuer must issue claim
 * @param validationRegex validation regex

 */
public record ClaimMetadata (
        String namespace, // only for mdoc, first in path - shallow credentials
        String name, // last in path mdoc, complete path SD-JWT VC - shallow credentials
        ClaimDataTypes type, // String, Number, Boolean, Binary, FullDate, DateTime, List, Map
        String mimeType, // only for type = binary
        Map<String, String> displayNames,
        boolean mandatory,
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
    public ClaimMetadata(String name, ClaimDataTypes type, Map<String, String> displayNames, boolean mandatory, String validationRegex) {
        this(null, name, type, null, displayNames, mandatory, validationRegex);
    }
    /**
     * Claim description, leaving out namespace (SD-JWT VC only). Do not use for sd-jwt of type Binary.
     *
     * @param name claim name
     * @param displayNames display names
     * @param mandatory issuer must issue claim
     * @param validationRegex validation regex
     */
    public ClaimMetadata(String namespace, String name, ClaimDataTypes type, Map<String, String> displayNames, boolean mandatory, String validationRegex) {
        this(namespace, name, type, null, displayNames, mandatory, validationRegex);
    }

    /**
     * Claim description, using string as type and leaving out namespace (SD-JWT VC only).
     *
     * @param name claim name
     * @param displayNames display names
     * @param mandatory issuer must issue claim
     * @param validationRegex validation regex
     */
    public ClaimMetadata(String name, Map<String, String> displayNames, boolean mandatory, String validationRegex) {
        this(null, name, ClaimDataTypes.STRING, null, displayNames, mandatory, validationRegex);
    }

    public String getDisplayName(String locale) {
        return displayNames.get(locale);
    }

    public List<String> path() {
        if (namespace != null) {
            return List.of(namespace, name);
        }
        return Collections.singletonList(name);
    }

}
