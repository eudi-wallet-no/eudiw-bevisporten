package no.idporten.eudiw.issuer.jwt;

import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("When reading claims by path")
class JwtClaimsTest {

    private static final String CLAIMS = """
            {
              "iss": "junit",
              "exp": 1303497700,
              "client_status": {
                "status": {
                  "status_list": {
                    "idx": 1337,
                    "uri": "https://revocation_url/wia-statuslists/42"
                  }
                },
                "exp": 1303497780
              },
              "types": {
                "string": "s",
                "integer": 42,
                "long": 4200000000,
                "float": 1.5,
                "double": 2.5,
                "boolean": true,
                "uri": "https://junit.example",
                "date": 1303497780,
                "strings": ["a", "b"],
                "list": ["a", 1],
                "object": {"foo": "bar"}
              }
            }
            """;

    private JWTClaimsSet claims;

    @BeforeEach
    void setUp() throws Exception {
        claims = JWTClaimsSet.parse(CLAIMS);
    }

    @DisplayName("then nested claims are read with the same types as JWTClaimsSet")
    @Test
    void testReadNestedClaimsOfAllTypes() {
        assertAll(
                () -> assertEquals(Optional.of("s"), JwtClaims.getStringClaim(claims, "types", "string")),
                () -> assertEquals(Optional.of(42), JwtClaims.getIntegerClaim(claims, "types", "integer")),
                () -> assertEquals(Optional.of(4200000000L), JwtClaims.getLongClaim(claims, "types", "long")),
                () -> assertEquals(Optional.of(1.5f), JwtClaims.getFloatClaim(claims, "types", "float")),
                () -> assertEquals(Optional.of(2.5d), JwtClaims.getDoubleClaim(claims, "types", "double")),
                () -> assertEquals(Optional.of(true), JwtClaims.getBooleanClaim(claims, "types", "boolean")),
                () -> assertEquals(Optional.of(URI.create("https://junit.example")), JwtClaims.getURIClaim(claims, "types", "uri")),
                () -> assertEquals(Optional.of(new Date(1303497780000L)), JwtClaims.getDateClaim(claims, "types", "date")),
                () -> assertArrayEquals(new String[]{"a", "b"}, JwtClaims.getStringArrayClaim(claims, "types", "strings").orElseThrow()),
                () -> assertEquals(Optional.of(List.of("a", "b")), JwtClaims.getStringListClaim(claims, "types", "strings")),
                () -> assertEquals(2, JwtClaims.getListClaim(claims, "types", "list").orElseThrow().size()),
                () -> assertEquals(Optional.of(Map.of("foo", "bar")), JwtClaims.getJSONObjectClaim(claims, "types", "object")),
                () -> assertEquals(Optional.of("s"), JwtClaims.getClaim(claims, "types", "string"))
        );
    }

    @DisplayName("then a status list reference is read from a deeply nested path")
    @Test
    void testReadStatusListReference() {
        assertAll(
                () -> assertEquals(Optional.of(1337), JwtClaims.getIntegerClaim(claims, "client_status", "status", "status_list", "idx")),
                () -> assertEquals(Optional.of(URI.create("https://revocation_url/wia-statuslists/42")), JwtClaims.getURIClaim(claims, "client_status", "status", "status_list", "uri")),
                () -> assertEquals(Optional.of(new Date(1303497780000L)), JwtClaims.getDateClaim(claims, "client_status", "exp"))
        );
    }

    @DisplayName("then a single claim name reads a top level claim")
    @Test
    void testReadTopLevelClaim() {
        assertAll(
                () -> assertEquals(Optional.of("junit"), JwtClaims.getStringClaim(claims, "iss")),
                () -> assertEquals(Optional.of(new Date(1303497700000L)), JwtClaims.getDateClaim(claims, "exp"))
        );
    }

    @DisplayName("then missing claims")
    @Nested
    class MissingClaims {

        @DisplayName("give empty when the last claim in the path is missing")
        @Test
        void testMissingLastClaim() {
            assertTrue(JwtClaims.getStringClaim(claims, "client_status", "missing").isEmpty());
        }

        @DisplayName("give empty when an intermediate claim in the path is missing")
        @Test
        void testMissingIntermediateClaim() {
            assertTrue(JwtClaims.getIntegerClaim(claims, "missing", "status", "status_list", "idx").isEmpty());
        }

    }

    @DisplayName("then invalid claims")
    @Nested
    class InvalidClaims {

        @DisplayName("throw when the last claim in the path has wrong type")
        @Test
        void testWrongTypeOfLastClaim() {
            InvalidClaimException e = assertThrows(InvalidClaimException.class,
                    () -> JwtClaims.getIntegerClaim(claims, "client_status", "status", "status_list", "uri"));
            assertEquals("Claim client_status.status.status_list.uri has invalid type", e.getMessage());
        }

        @DisplayName("throw when an intermediate claim in the path is not a JSON object")
        @Test
        void testIntermediateClaimNotAnObject() {
            InvalidClaimException e = assertThrows(InvalidClaimException.class,
                    () -> JwtClaims.getIntegerClaim(claims, "client_status", "exp", "idx"));
            assertEquals("Claim client_status.exp is not a JSON object", e.getMessage());
        }

        @DisplayName("throw when the path is empty")
        @Test
        void testEmptyPath() {
            assertThrows(IllegalArgumentException.class, () -> JwtClaims.getStringClaim(claims));
        }

    }

}
