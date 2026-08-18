package no.idporten.eudiw.verifier.statuslist;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;

public class StatusListTestUtil {

    public static final byte[] HMAC_SECRET = "12345678901234567890123456789012".getBytes();

    private static final String RSA_PRIVATE_KEY_PKCS8_BASE64 =
            "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCg05q8VU2w1Q0yGkO1Np2ivkZjT9XVglJq2I6DgRXus6MQ1vGuQwC5BrllXxli/5v9VJ2wdbGL+FwEyOA1ux7C++MpoLzlf7D4R7J5JVdXJGStchtMGpCC2LE07AczL09th8MWf01Q7IuDeKmZkjIvLfoGnk2SjGi3lMxFgQCoucLFwpvsLczvbEUzI5pi/nxJlolINC89QHQRe+lXKPG3TWOaG9CuzybF5GRXFJYtBQYwcz21xsXjBAXQtoLTWR8JfYFGl8WbLfJNORKWRvv+FjXH0oztruHzlELY2S1O/DNaRSh2zQ1KAY56lQXxWiybtoRUlYvj4pK4SGeEVOVVAgMBAAECggEAKPY8TwObChL5jW1LGXiTpcO4wcqvt/W9cCFACxZxs55lRgC5BZ1jLb0cs0kJekGIRXmrwCn50qCrLzzKfmTubNMjBJNACWQAevwn5Nsx56wJSHPrp/KMJAd0+vmyy8KdBFSzx6Mc2iOlVRMCf27RAVtAdzcSouTINxna/UjiFhbyu5MrpdNUB6UFHZIBjn2mg2hBOVjqv3svKbyZ2aQk9UDaogXW0X5khVMuC9oWP+2WfK15brIO+dshM+hFL7+Dda/OptXfAZ/GSZVb2C5Be59EYpPvq/xapzo7/bHBDBfXeGFRqbRK+FV2PXjM6yihkvww+9pluSpAwmvHdPBB8QKBgQDMfWdCAxmdXiUh68FOXPixoNmWVmmhmW8b2T5hUNZ8gb0cuv00idthkQh6cYIaBArzUER+3hCxXcKleVDF5dUe/4zt4tr4I+7L0sg6jOH/Ola63GPQP47UP1Co5CiIKKUgLfoFhGEOJMSmVrq1q7qhzUuCUc7ApnEH+kRzNMNjbwKBgQDJVo9imz7azVH8Cd3c2wfcrcmTpf7CBiCAscWa0uIH3i2SWGUVnwOBbDLKUWryCaGTHfWZayKjZf9htK7obB/k92ydGmuNHu0R2E6Hdv0pw6zUpEPxn6WgVE3buHtqWetLm89Os8G/w8BVWocHg+2AsKNDZ+KuuHGyTPHzcOJRewKBgD1uTt7d34wPBEi0clYASBXUpIktXH5XgbF0CfFiP40XzKAc7IQkBevBjjJ6dwMpw8BklK8oNwuRhy/+ye0ppmSxkLzGMdHpuYsxv0UXnaz28achHjspXNcWTjzujd6Zl0GbjkiqBB4jnY/67gw1ktOYwN5dhScK2Vn0LSwO5cOxAoGBALw0bAhS8qh5sZ+WYYxoOqsw0PNyAfjA3XUvuHYHlz6fk53vf158rDw5NiklfoEOO7lYEhL9sP1pBRcCXVXqbM4N87vPzKW4OQerWY0Lz49Q2KhVfSXKhHqhE77Gbqd1spuU5G6/XeeyRoIDA6Ik0sivVY5W9/E3fSCdCLb12LktAoGBAJtJzJUY1uMuT/r00KZCJap8cgCGweav9iI9en0lqOqhp/X00kPuqHBftdX9nt1lSrbNYvD61Xu7ik323B1aJKdWGDYtR4fdwcc/VlDNbOrlBTyya08plzBsGntRev5UuWSmVFg4Qsh0PLqKg++muDv6C1bqamJphomCo1TqySFO";
    private static final String CERT_DER_BASE64 =
            "MIICmjCCAYICCQDQH0v/Yhne/DANBgkqhkiG9w0BAQsFADAPMQ0wCwYDVQQDDAR0ZXN0MB4XDTI2MDgxMzA1NTU0M1oXDTI3MDgxMzA1NTU0M1owDzENMAsGA1UEAwwEdGVzdDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKDTmrxVTbDVDTIaQ7U2naK+RmNP1dWCUmrYjoOBFe6zoxDW8a5DALkGuWVfGWL/m/1UnbB1sYv4XATI4DW7HsL74ymgvOV/sPhHsnklV1ckZK1yG0wakILYsTTsBzMvT22HwxZ/TVDsi4N4qZmSMi8t+gaeTZKMaLeUzEWBAKi5wsXCm+wtzO9sRTMjmmL+fEmWiUg0Lz1AdBF76Vco8bdNY5ob0K7PJsXkZFcUli0FBjBzPbXGxeMEBdC2gtNZHwl9gUaXxZst8k05EpZG+/4WNcfSjO2u4fOUQtjZLU78M1pFKHbNDUoBjnqVBfFaLJu2hFSVi+PikrhIZ4RU5VUCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEADY9i+y+Dw4Ma6HlHyrKovlPwwSNHkpQHXekIl0+3KkHEtv/GH0nNHtg1zbAgK4QY8inJNoYeec6MhDFuvINBhkYbEthFMN+qOEauCA8duOCDjB7KYkf042ManQj1O5Z5svLMZy8d6z0ZLbBQnS5B4VMBl3/Tak2bdRWvNRzxhA9NN4fnallccFbUOYgwXn+kk5lEsVgnzq26m5Ycx/t69/ozL9SIzColTHhzuylkIlIN0xXDgkIOi5YjuHfSm8qNSv3aPlFRM4clJKDiRcqhiwrdaGrqAlFk7+0KxCzNiKvqFQ/fGXQ6p3gESbzcJPEZ5cYUMUIKBL/S7NlHB9vpvg==";

    public static String createSignedStatusListJwtWithHmac(URI uri, Instant now, int bits, byte[] statuses) throws Exception {
        String lst = base64UrlDeflated(statuses);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(uri.toString())
                .issueTime(Date.from(now.minusSeconds(30)))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .claim("status_list", Map.of("bits", bits, "lst", lst))
                .build();

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256)
                        .type(new JOSEObjectType("statuslist+jwt"))
                        .build(),
                claimsSet
        );
        jwt.sign(new MACSigner(HMAC_SECRET));
        return jwt.serialize();
    }

    public static String createSignedStatusListJwtWithRsa(URI uri, Instant now, int idx, int statusAtIdx) throws Exception {
        byte statusByte = (byte) (statusAtIdx << idx);
        String lst = base64UrlDeflated(new byte[]{statusByte});
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(uri.toString())
                .issueTime(Date.from(now.minusSeconds(30)))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .claim("status_list", Map.of("bits", 1, "lst", lst))
                .build();

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .type(new JOSEObjectType("statuslist+jwt"))
                        .x509CertChain(List.of(new Base64(CERT_DER_BASE64)))
                        .build(),
                claimsSet
        );
        jwt.sign(new RSASSASigner(loadRsaPrivateKey()));
        return jwt.serialize();
    }

    public static String base64UrlDeflated(byte[] bytes) throws IOException {
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflater = new DeflaterOutputStream(compressed)) {
            deflater.write(bytes);
        }
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(compressed.toByteArray());
    }

    public static RSAPrivateKey loadRsaPrivateKey() throws Exception {
        byte[] keyBytes = java.util.Base64.getDecoder().decode(RSA_PRIVATE_KEY_PKCS8_BASE64);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private StatusListTestUtil() {
    }
}
