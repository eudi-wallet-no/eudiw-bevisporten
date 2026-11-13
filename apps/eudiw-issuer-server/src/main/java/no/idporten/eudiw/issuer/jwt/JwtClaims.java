package no.idporten.eudiw.issuer.jwt;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;

import java.net.URI;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Utilities for reading claim values from JWT claims sets.   path of claim names.
 */
public final class JwtClaims {

    private JwtClaims() {
    }

    /**
     * Claims set of the jwt, converting ParseException as InvalidClaimException.
     */
    public static JWTClaimsSet claimsSet(JWT jwt) {
        try {
            return jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new InvalidClaimException("Invalid JWT claims", e);
        }
    }

    public static Optional<Object> getClaim(JWTClaimsSet claims, String... path) {
        return get(claims, JWTClaimsSet::getClaim, path);
    }

    public static Optional<String> getStringClaim(JWTClaimsSet claims, String... path) {
        return get(claims, JWTClaimsSet::getStringClaim, path);
    }

    public static Optional<List<Object>> getListClaim(JWTClaimsSet claims, String... path) {
        return get(claims, JWTClaimsSet::getListClaim, path);
    }

    public static Optional<String[]> getStringArrayClaim(JWTClaimsSet claims, String... path) {
        return get(claims, JWTClaimsSet::getStringArrayClaim, path);
    }

    public static Optional<List<String>> getStringListClaim(JWTClaimsSet claims, String... path) {
        return get(claims, JWTClaimsSet::getStringListClaim, path);
    }

    public static Optional<URI> getURIClaim(JWTClaimsSet claims, String... path) {
        return get(claims, JWTClaimsSet::getURIClaim, path);
    }

    public static Optional<Boolean> getBooleanClaim(JWTClaimsSet claims, String... path) {
        return get(claims, JWTClaimsSet::getBooleanClaim, path);
    }

    public static Optional<Integer> getIntegerClaim(JWTClaimsSet claims, String... path) {
        return get(claims, JWTClaimsSet::getIntegerClaim, path);
    }

    public static Optional<Long> getLongClaim(JWTClaimsSet claims, String... path) {
        return get(claims, JWTClaimsSet::getLongClaim, path);
    }

    public static Optional<Date> getDateClaim(JWTClaimsSet claims, String... path) {
        return get(claims, JWTClaimsSet::getDateClaim, path);
    }

    public static Optional<Float> getFloatClaim(JWTClaimsSet claims, String... path) {
        return get(claims, JWTClaimsSet::getFloatClaim, path);
    }

    public static Optional<Double> getDoubleClaim(JWTClaimsSet claims, String... path) {
        return get(claims, JWTClaimsSet::getDoubleClaim, path);
    }

    public static Optional<Map<String, Object>> getJSONObjectClaim(JWTClaimsSet claims, String... path) {
        return get(claims, JWTClaimsSet::getJSONObjectClaim, path);
    }

    @FunctionalInterface
    private interface ClaimGetter<T> {
        T get(JWTClaimsSet claims, String name) throws ParseException;
    }

    private static <T> Optional<T> get(JWTClaimsSet claims, ClaimGetter<T> getter, String... path) {
        Objects.requireNonNull(claims, "claims");
        if (path == null || path.length == 0) {
            throw new IllegalArgumentException("Claim path must not be empty");
        }
        Map<String, Object> current = claims.getClaims();
        for (int i = 0; i < path.length - 1; i++) {
            Object value = current.get(path[i]);
            if (value == null) {
                return Optional.empty();
            }
            if (!(value instanceof Map<?, ?>)) {
                throw new InvalidClaimException("Claim %s is not a JSON object".formatted(toPathString(path, i + 1)));
            }
            current = asJSONObject(value);
        }
        String name = path[path.length - 1];
        Object value = current.get(name);
        if (value == null) {
            return Optional.empty();
        }
        try {
            // Reuse the type conversion of JWTClaimsSet for the last claim in the path
            return Optional.ofNullable(getter.get(new JWTClaimsSet.Builder().claim(name, value).build(), name));
        } catch (ParseException e) {
            throw new InvalidClaimException("Claim %s has invalid type".formatted(toPathString(path, path.length)), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asJSONObject(Object value) {
        return (Map<String, Object>) value;
    }

    private static String toPathString(String[] path, int length) {
        return String.join(".", List.of(path).subList(0, length));
    }

}
