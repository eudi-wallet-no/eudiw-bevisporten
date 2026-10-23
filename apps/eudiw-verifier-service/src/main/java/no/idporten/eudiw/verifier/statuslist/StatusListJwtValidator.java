package no.idporten.eudiw.verifier.statuslist;

import com.nimbusds.jose.*;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.verifier.VerificationException;

import java.net.URI;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * I henhold til
 * <a href="https://datatracker.ietf.org/doc/draft-ietf-oauth-status-list/21/">Token Status List V21</a>
 */
public class StatusListJwtValidator {

    private static final String STATUS_LIST_JWT_TYP = "statuslist+jwt";
    private static final String ERROR_CODE = "invalid_request";

    private final Set<JWSAlgorithm> allowedAlgorithms;
    private final Duration clockSkew;

    public StatusListJwtValidator(Set<JWSAlgorithm> allowedAlgorithms, Duration clockSkew) {
        this.allowedAlgorithms = allowedAlgorithms;
        this.clockSkew = clockSkew;
    }

    public int validateAndResolveStatus(URI expectedUri, int idx, String statusListJwt, Instant now, JWSVerifier verifier) {
        if (expectedUri == null) {
            throw new VerificationException(ERROR_CODE, "Expected status list uri is required");
        }
        if (statusListJwt == null || statusListJwt.isBlank()) {
            throw new VerificationException(ERROR_CODE, "Status list JWT is required");
        }
        if (now == null) {
            throw new VerificationException(ERROR_CODE, "Current time is required");
        }
        if (verifier == null) {
            throw new VerificationException(ERROR_CODE, "JWS verifier is required");
        }

        SignedJWT jwt = parseJwt(statusListJwt);
        validateHeader(jwt.getHeader());
        verifySignature(jwt, verifier);
        JWTClaimsSet claims = parseClaims(jwt);

        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new VerificationException(ERROR_CODE, "Status list JWT is missing required sub claim");
        }
        if (!expectedUri.toString().equals(subject)) {
            throw new VerificationException(ERROR_CODE, "Status list JWT sub does not match expected status list uri");
        }

        if (claims.getIssueTime() == null) {
            throw new VerificationException(ERROR_CODE, "Status list JWT is missing required iat claim");
        }
        Instant issuedAt = claims.getIssueTime().toInstant();
        if (issuedAt.isAfter(now.plus(clockSkew))) {
            throw new VerificationException(ERROR_CODE, "Status list JWT iat is in the future");
        }

        Instant expirationTime;
        if (claims.getExpirationTime() != null) {
            expirationTime = claims.getExpirationTime().toInstant();
            if (!now.minus(clockSkew).isBefore(expirationTime)) {
                throw new VerificationException(ERROR_CODE, "Status list JWT is expired");
            }
        }

        Map<String, Object> statusList;
        try {
            statusList = claims.getJSONObjectClaim("status_list");
        } catch (ParseException e) {
            throw new VerificationException(ERROR_CODE, "Status list JWT status_list claim could not be parsed");
        }
        if (statusList == null) {
            throw new VerificationException(ERROR_CODE, "Status list JWT is missing required status_list claim");
        }

        return StatusListDecoder.resolveStatus(idx, statusList.get("bits"), statusList.get("lst"));
    }

    private SignedJWT parseJwt(String statusListJwt) {
        try {
            return SignedJWT.parse(statusListJwt);
        } catch (ParseException e) {
            throw new VerificationException(ERROR_CODE, "Status list JWT is not a valid compact signed JWT");
        }
    }

    private void validateHeader(JWSHeader header) {
        JOSEObjectType typ = header.getType();
        if (typ == null || !STATUS_LIST_JWT_TYP.equals(typ.toString())) {
            throw new VerificationException(ERROR_CODE, "Status list JWT typ must be statuslist+jwt");
        }

        JWSAlgorithm algorithm = header.getAlgorithm();
        if (algorithm == null || JWSAlgorithm.NONE.equals(algorithm) || !allowedAlgorithms.contains(algorithm)) {
            throw new VerificationException(ERROR_CODE, "Status list JWT algorithm is not allowed");
        }
    }

    private void verifySignature(SignedJWT jwt, JWSVerifier verifier) {
        try {
            if (!jwt.verify(verifier)) {
                throw new VerificationException(ERROR_CODE, "Status list JWT signature validation failed");
            }
        } catch (JOSEException e) {
            throw new VerificationException(ERROR_CODE, "Status list JWT signature validation failed");
        }
    }

    private JWTClaimsSet parseClaims(SignedJWT jwt) {
        try {
            return jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new VerificationException(ERROR_CODE, "Status list JWT claims could not be parsed");
        }
    }

}
