package no.idporten.eudiw.issuer.oauth2;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.JOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jwt.proc.ExpiredJWTException;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.validators.AbstractJWTValidator;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.exception.ErrorCode;

import java.net.URI;
import java.util.Set;

public class AccessTokenValidator extends AbstractJWTValidator {

    public AccessTokenValidator(Issuer expectedIssuer, JWSKeySelector jwsKeySelector) {
        super(JOSEObjectType.JWT, expectedIssuer, new ClientID("IGNORE"), jwsKeySelector, null);
    }

    @SuppressWarnings("unchecked")
    public JWT validate(JWT accessToken, URI acceptedAudience) {
        if (accessToken == null) {
            throw new IssuerServerException(ErrorCode.UNAUTHORIZED_INVALID_REQUEST, "Missing access token");
        }
        if (! (accessToken instanceof SignedJWT)) {
            throw new IssuerServerException(ErrorCode.INVALID_TOKEN, "Unsigned access token");
        }
        try {
            SignedJWT signedJWT = (SignedJWT) accessToken;
            ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
            jwtProcessor.setJWSKeySelector((JWSKeySelector<SecurityContext>) this.getJWSKeySelector());
            jwtProcessor.setJWTClaimsSetVerifier(new AccessTokenClaimsVerifier<>(
                    Set.of(String.valueOf(acceptedAudience)),
                    Set.of("iss", "aud", "scope", "iat", "exp")));
            jwtProcessor.setJWSTypeVerifier(createJWSTypeVerifier());
            jwtProcessor.process(accessToken, (SecurityContext) null);
            return signedJWT;
        } catch (ExpiredJWTException e) {
            throw new IssuerServerException(ErrorCode.INVALID_TOKEN, "Expired access token");
        } catch (BadJOSEException e) {
            throw new IssuerServerException(ErrorCode.INVALID_TOKEN, "Invalid token format", e);
        } catch (JOSEException e) {
            throw new IssuerServerException(ErrorCode.INVALID_TOKEN, "Invalid token signature");
        }
    }

    private static JOSEObjectTypeVerifier<SecurityContext> createJWSTypeVerifier() {
        return (joseObjectType, _) -> {
            if (joseObjectType != null && !joseObjectType.equals(JOSEObjectType.JWT) && !joseObjectType.equals(new JOSEObjectType("at+jwt"))) {
                throw new BadJOSEException("JOSE header typ (type) " + joseObjectType + " not allowed");
            }
        };
    }

}
