package no.idporten.eudiw.oauth2.server.util;

import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.text.ParseException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When reading claims from a JWT")
public class JwtClaimsUtilsTest {

    private final JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .claim("a", JsonObjectBuilder.builder()
                    .addAttribute("b", JsonObjectBuilder.builder()
                            .addAttribute("uri", "https://statuslist.junit.eidas2sandkasse.dev/1")
                            .addAttribute("idx", 42L)
                            .build())
                    .addAttribute("text", "not-an-object")
                    .build())
            .build();

    @DisplayName("then nested claims are read by path")
    @Test
    void testNestedClaims() throws Exception {
        assertAll(
                () -> assertEquals(Optional.of(URI.create("https://statuslist.junit.eidas2sandkasse.dev/1")), JwtClaimsUtils.getURIClaim(claims, "a", "b", "uri")),
                () -> assertEquals(Optional.of(42L), JwtClaimsUtils.getLongClaim(claims, "a", "b", "idx"))
        );
    }

    @DisplayName("then missing claims are empty")
    @Test
    void testMissingClaims() throws Exception {
        assertAll(
                () -> assertTrue(JwtClaimsUtils.getLongClaim(claims, "a", "b", "missing").isEmpty()),
                () -> assertTrue(JwtClaimsUtils.getLongClaim(claims, "missing", "b", "idx").isEmpty())
        );
    }

    @DisplayName("then a path through a claim that is not a JSON object fails")
    @Test
    void testPathThroughNonObject() {
        ParseException e = assertThrows(ParseException.class, () -> JwtClaimsUtils.getLongClaim(claims, "a", "text", "idx"));
        assertTrue(e.getMessage().contains("a.text"));
    }

    @DisplayName("then a claim with wrong type fails")
    @Test
    void testWrongType() {
        assertThrows(ParseException.class, () -> JwtClaimsUtils.getLongClaim(claims, "a", "b", "uri"));
    }

    @DisplayName("then an empty path is not allowed")
    @Test
    void testEmptyPath() {
        assertThrows(IllegalArgumentException.class, () -> JwtClaimsUtils.getLongClaim(claims));
    }

}
