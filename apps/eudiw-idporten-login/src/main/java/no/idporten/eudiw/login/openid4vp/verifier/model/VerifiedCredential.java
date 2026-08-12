package no.idporten.eudiw.login.openid4vp.verifier.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import no.idporten.eudiw.login.openid4vp.InvalidVerificationException;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record VerifiedCredential(
        Map<String, Object> claims
) {

    public String getStringClaim(boolean mandatory, String... path) {
        String value = getClaim(mandatory, path);
        if (mandatory && ! StringUtils.hasText(value)) {
            throw new InvalidVerificationException("Missing mandatory claim", "Missing mandatory claim for path %s".formatted(Arrays.toString(path)));
        }
        return value;
    }

    private <T> T getClaim(boolean mandatory, String... path) {
        if (path == null || path.length == 0) {
            throw new InvalidVerificationException("Invalid claim path", "Claim path cannot be empty");
        }
        final Object value;
        try {
            value = processPath(0, claims(), path);
        } catch (InvalidVerificationException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidVerificationException("Invalid claim format", "Invalid claim format for path %s".formatted(Arrays.toString(path)));
        }
        if (mandatory && value == null) {
            throw new InvalidVerificationException("Missing mandatory claim", "Missing mandatory claim for path %s".formatted(Arrays.toString(path)));
        }
        return (T) value;
    }

    /**
     * Recursively process path and return referenced data element.
     */
    private Object processPath(int idx, Map<String, Object> claims, String... path) {
        if (claims == null) {
            throw new InvalidVerificationException("Invalid claim path", "No value found for position %d in path %s".formatted(idx, Arrays.toString(path)));
        }
        if (idx == path.length -1) {
            return claims.get(path[idx]);
        }
        return processPath(idx + 1, (Map<String, Object>) claims.get(path[idx]), path);
    }

}
