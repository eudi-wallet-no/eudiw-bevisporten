package no.idporten.eudiw.issuer.oauth2;

import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.util.DateUtils;

import java.util.Date;
import java.util.Set;

public class AccessTokenClaimsVerifier<C extends SecurityContext> extends DefaultJWTClaimsVerifier<C> {

    public AccessTokenClaimsVerifier(Set<String> requiredClaims) {
        super(null, null, requiredClaims, null);
    }

    @Override
    public void verify(JWTClaimsSet claimsSet, C context) throws BadJWTException {
        super.verify(claimsSet, context);
        Date now = this.currentTime();
        Date issueTime = claimsSet.getIssueTime();
        if (issueTime != null && !DateUtils.isBefore(issueTime, now, this.getMaxClockSkew())) {
            throw new BadJWTException("JWT before issue time");
        }
    }

}
