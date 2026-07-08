package no.idporten.eudiw.login.openid4vp.verifier.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import no.idporten.eudiw.login.openid4vp.InvalidVerificationException;
import org.springframework.util.StringUtils;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record VerifiedCredential(
        Map<String, Object> claims
) {

    public String getStringClaim(String path, boolean mandatory) {
        String value = getClaim(path, mandatory);
        if (mandatory && ! StringUtils.hasText(value)) {
            throw new InvalidVerificationException("Missing mandatory claim", "Missing mandatory claim for path %s".formatted(path));
        }
        return value;
    }

    public <T> T getClaim(String path, boolean mandatory) {
        final T value;
        try {
            value = (T) claims().get(path);
        } catch (Exception e) {
            throw new InvalidVerificationException("Invalid claim format", "Invalid claim format for path %s".formatted(path));
        }
        if (mandatory && value == null) {
            throw new InvalidVerificationException("Missing mandatory claim", "Missing mandatory claim for path %s".formatted(path));
        }
        return value;
    }

}
