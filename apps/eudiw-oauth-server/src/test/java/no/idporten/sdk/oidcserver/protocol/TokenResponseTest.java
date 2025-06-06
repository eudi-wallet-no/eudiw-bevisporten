package no.idporten.sdk.oidcserver.protocol;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When generating token responses")
public class TokenResponseTest {

    @Test
    @DisplayName("then standard attributes are included")
    public void testBuildTokenResponse() {
        TokenResponse tokenResponse = TokenResponse.builder()
                .expiresInSeconds(1)
                .tokenType("bjørnar")
                .idToken("id")
                .accessToken("at")
                .build();
        assertAll(
                () -> assertEquals(1l, tokenResponse.getExpiresInSeconds()),
                () -> assertEquals("bjørnar", tokenResponse.getTokenType()),
                () -> assertEquals("id", tokenResponse.getIdToken()),
                () -> assertEquals("at", tokenResponse.getAccessToken())
        );
    }

    @Test
    @DisplayName("then standard attributes are included in the json object representation")
    public void testBuildTokenResponseJson() {
        Map<String, Object> jsonObject = TokenResponse.builder()
                .expiresInSeconds(1)
                .tokenType("bjørnar")
                .idToken("id")
                .accessToken("at")
                .build().toJsonObject();
        assertAll(
                () -> assertEquals(1l, jsonObject.get("expires_in")),
                () -> assertEquals("bjørnar", jsonObject.get("token_type")),
                () -> assertEquals("id", jsonObject.get("id_token")),
                () -> assertEquals("at", jsonObject.get("access_token"))
        );
    }

    @Test
    @DisplayName("then audit data contains tokens, token_type and expires_in")
    public void testAuditData() {
        TokenResponse tokenResponse = TokenResponse.builder()
                .expiresInSeconds(1)
                .tokenType("bjørnar")
                .idToken("id")
                .accessToken("at")
                .build();
        AuditData auditData = tokenResponse.getAuditData();
        assertAll(
                () -> assertEquals(1l, tokenResponse.getExpiresInSeconds()),
                () -> assertEquals("bjørnar", tokenResponse.getTokenType()),
                () -> assertEquals("id", tokenResponse.getIdToken()),
                () -> assertEquals("at", tokenResponse.getAccessToken())
        );
    }

    @Test
    @DisplayName("then opaque access_token is masked in audit data")
    public void testAuditTruncateOpaqueAccessToken() {
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("1234567890abcdefghijklmnopqrstu")
                .build();
        assertEquals("1234567890...", tokenResponse.getAuditData().getAttribute("access_token"));
    }

    @Test
    @DisplayName("then JWT access_token is masked in audit data")
    public void testAuditTruncateJwtAccessToken() {
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("header.body.sign")
                .build();
        assertEquals("header.body...", tokenResponse.getAuditData().getAttribute("access_token"));
    }

    @Test
    @DisplayName("then short opaque access_token is not masked in audit data")
    public void testAuditSkipTruncateShortOpaqueAccessToken() {
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("short")
                .build();
        assertEquals("short", tokenResponse.getAuditData().getAttribute("access_token"));
    }


}
