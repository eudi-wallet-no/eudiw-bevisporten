package no.idporten.sdk.oidcserver.protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When including access_token in audit data")
public class AuditDataTest {

    @Test
    @DisplayName("then empty tokens are removed")
    public void testMaskNullAccessToken() {
        AuditData auditData = AuditData.builder().accessToken(null).build();
        assertNull(auditData.getAttribute("access_token"));
    }

    @Test
    @DisplayName("then short tokens are included as is")
    public void testMaskShortAccessToken() {
        AuditData auditData = AuditData.builder().accessToken("abc").build();
        assertEquals("abc", auditData.getAttribute("access_token"));
    }

    @Test
    @DisplayName("then opaque tokens are masked by chopping after 10 characters")
    public void testMaskOpaqueAccessToken() {
        AuditData auditData = AuditData.builder().accessToken("abcdefghijklmnopqrstu").build();
        assertEquals("abcdefghij...", auditData.getAttribute("access_token"));
    }

    @Test
    @DisplayName("then JWK tokens are masked by removing the signature")
    public void testMaskJWTAccessToken() {
        AuditData auditData = AuditData.builder().accessToken("header.body.sign").build();
        assertEquals("header.body...", auditData.getAttribute("access_token"));
    }

}
