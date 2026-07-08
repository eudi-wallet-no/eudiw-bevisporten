package no.idporten.eudiw.login.openid4vp.verifier.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import no.idporten.sdk.oidcserver.OAuth2Exception;
import org.springframework.http.HttpStatus;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record VerifiedCredential(
        Map<String, Object> claims
) {

    public String getStringClaim(String path, boolean mandatory) {
        return getClaim(path, mandatory);
    }

    public <T> T getClaim(String path, boolean mandatory) {
        final T value;
        try {
            value = (T) claims().get(path);
        } catch (Exception e) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Invalid claim format", HttpStatus.BAD_REQUEST.value(), e);
        }
        if (mandatory && value == null) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Missing mandatory claim", HttpStatus.BAD_REQUEST.value());
        }
        return value;
    }



}
