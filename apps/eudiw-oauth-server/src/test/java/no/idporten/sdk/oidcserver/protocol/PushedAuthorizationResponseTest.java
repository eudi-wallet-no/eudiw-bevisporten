package no.idporten.sdk.oidcserver.protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When handling pushed authorization responses")
public class PushedAuthorizationResponseTest {

    @Test
    @DisplayName("then attributes request_uri and expires_in are included")
    public void testBuildPushedAuthorizationResponse() {
        PushedAuthorizationResponse pushedAuthorizationResponse = PushedAuthorizationResponse.builder()
                .expiresIn(60)
                .requestUri("ru")
                .build();
        assertAll(
                () -> assertEquals("ru", pushedAuthorizationResponse.getRequestUri()),
                () -> assertEquals(60, pushedAuthorizationResponse.getExpiresIn())
        );
    }

    @Test
    @DisplayName("then audit data includes response attributes")
    public void testAuditData() {
        PushedAuthorizationResponse pushedAuthorizationResponse = PushedAuthorizationResponse.builder()
                .expiresIn(60)
                .requestUri("ru")
                .build();
        AuditData auditData = pushedAuthorizationResponse.getAuditData();
        assertAll(
                () -> assertEquals(2, auditData.getAttributes().size()),
                () -> assertEquals("ru", auditData.getAttribute("request_uri")),
                () -> assertEquals(60l, auditData.getAttribute("expires_in"))
        );
    }

}
