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
import org.springframework.http.HttpStatus;

import java.util.Set;

public class AccessTokenValidator extends AbstractJWTValidator {


    public AccessTokenValidator(Issuer expectedIssuer, JWSKeySelector jwsKeySelector) {
        super(JOSEObjectType.JWT, expectedIssuer, new ClientID("IGNORE"), jwsKeySelector, null);
    }

    public JWT validate(JWT accessToken) {
        if (accessToken == null) {
            throw new IssuerServerException("invalid_request", "Missing access token", HttpStatus.UNAUTHORIZED);
        }
        if (! (accessToken instanceof SignedJWT)) {
            throw new IssuerServerException("invalid_token", "Unsigned access token", HttpStatus.UNAUTHORIZED);
        }
        try {
            SignedJWT signedJWT = (SignedJWT) accessToken;
            ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
            jwtProcessor.setJWSKeySelector(this.getJWSKeySelector());
            jwtProcessor.setJWTClaimsSetVerifier(new AccessTokenClaimsVerifier<>(
                    Set.of("iss", "aud", "sub", "scope", "iat", "exp")
                    ));
            jwtProcessor.setJWSTypeVerifier(createJWSTypeVerifier());
            jwtProcessor.process(accessToken, (SecurityContext) null);
            return signedJWT;
        } catch (ExpiredJWTException e) {
            throw new IssuerServerException("invalid_token", "Expired access token", HttpStatus.UNAUTHORIZED);
        } catch (BadJOSEException e) {
            throw new IssuerServerException("invalid_token", "Invalid token format", HttpStatus.UNAUTHORIZED, e);
        } catch (JOSEException e) {
            throw new IssuerServerException("invalid_token", "Invalid token signature", HttpStatus.UNAUTHORIZED);
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
