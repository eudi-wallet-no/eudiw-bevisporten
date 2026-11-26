package no.idporten.eudiw.oauth2.server.util;

import com.nimbusds.jwt.JWTClaimsSet;

import java.net.URI;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads claim values from JWT claims sets, also nested claims given as a path of claim names.
 * Throws ParseException if a claim is present, but has the wrong type, like the getters in JWTClaimsSet.
 */
public class JwtClaimsUtils {

    private JwtClaimsUtils() {
    }

    public static Optional<URI> getURIClaim(JWTClaimsSet claims, String... path) throws ParseException {
        return get(claims, JWTClaimsSet::getURIClaim, path);
    }

    public static Optional<Long> getLongClaim(JWTClaimsSet claims, String... path) throws ParseException {
        return get(claims, JWTClaimsSet::getLongClaim, path);
    }

    @FunctionalInterface
    private interface ClaimGetter<T> {
        T get(JWTClaimsSet claims, String name) throws ParseException;
    }

    private static <T> Optional<T> get(JWTClaimsSet claims, ClaimGetter<T> getter, String... path) throws ParseException {
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
                throw new ParseException("Claim %s is not a JSON object".formatted(toPathString(path, i + 1)), 0);
            }
            current = asJSONObject(value);
        }
        String name = path[path.length - 1];
        Object value = current.get(name);
        if (value == null) {
            return Optional.empty();
        }
        // Reuse the type conversion of JWTClaimsSet for the last claim in the path
        return Optional.ofNullable(getter.get(new JWTClaimsSet.Builder().claim(name, value).build(), name));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asJSONObject(Object value) {
        return (Map<String, Object>) value;
    }

    private static String toPathString(String[] path, int length) {
        return String.join(".", List.of(path).subList(0, length));
    }

}
