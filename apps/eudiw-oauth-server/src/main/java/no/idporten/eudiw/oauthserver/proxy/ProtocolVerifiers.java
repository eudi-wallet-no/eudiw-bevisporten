package no.idporten.eudiw.oauthserver.proxy;

import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.openid.connect.sdk.Nonce;
import jakarta.servlet.http.HttpSession;
import no.idporten.sdk.oidcserver.OAuth2Exception;
import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.util.Objects;

/**
 * Verification objects for OIDC protocol.
 */
public record ProtocolVerifiers(State state, Nonce nonce, CodeVerifier codeVerifier) implements Serializable {

    public static ProtocolVerifiers forLogin() {
        return new ProtocolVerifiers(new State(), new Nonce(), new CodeVerifier());
    }

    public ProtocolVerifiers {
        Objects.requireNonNull(state, "state must have value");
    }

    public void toHttpSession(HttpSession session) {
        session.setAttribute(sessionAttributeName(), this);
    }

    public void removeFromSession(HttpSession session) {
        session.removeAttribute(sessionAttributeName());
    }

    private static String sessionAttributeNameFromClass() {
        return ProtocolVerifiers.class.getName();
    }

    public static ProtocolVerifiers fromSession(HttpSession httpSession) {
        ProtocolVerifiers protocolVerifiers = (ProtocolVerifiers) httpSession.getAttribute(sessionAttributeNameFromClass());
        if (protocolVerifiers == null) {
            throw new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Missing OIDC protocol verifiers.  Session is invalid", HttpStatus.BAD_REQUEST.value());
        }
        protocolVerifiers.removeFromSession(httpSession);
        return protocolVerifiers;
    }

    public String sessionAttributeName() {
        return sessionAttributeNameFromClass();
    }

}
