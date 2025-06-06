package no.idporten.sdk.oidcserver.protocol;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When generating authorization responses")
public class AuthorizationResponseTest {

    @Test
    @DisplayName("then response_mode = form_post can be detected")
    public void testIsFormPost() {
        AuthorizationResponse authorizationResponse = AuthorizationResponse.builder()
                .responseMode("form_post")
                .build();
        assertTrue(authorizationResponse.isFormPost());
        assertFalse(authorizationResponse.isQuery());
        assertFalse(authorizationResponse.isQueryJwt());
    }

    @Test
    @DisplayName("then response_mode = query can be detected")
    public void testIsQuery() {
        AuthorizationResponse authorizationResponse = AuthorizationResponse.builder()
                .responseMode("query")
                .build();
        assertTrue(authorizationResponse.isQuery());
        assertFalse(authorizationResponse.isFormPost());
        assertFalse(authorizationResponse.isQueryJwt());
    }

    @Test
    @DisplayName("then response_mode = query.jwt can be detected")
    public void testIsQueryJwt() {
        AuthorizationResponse authorizationResponse = AuthorizationResponse.builder()
                .responseMode("query.jwt")
                .build();
        assertTrue(authorizationResponse.isQueryJwt());
        assertFalse(authorizationResponse.isQuery());
        assertFalse(authorizationResponse.isFormPost());
    }

    @Test
    @DisplayName("then response_mode = query is default")
    public void testIsQueryByDefault() {
        AuthorizationResponse authorizationResponse = AuthorizationResponse.builder().build();
        assertTrue(authorizationResponse.isQuery());
        assertFalse(authorizationResponse.isFormPost());
    }

    @Test
    @DisplayName("then audit data contains the response_mode and the redirect_uri, and the authorization_code, state and issuer parameters")
    public void testAuditData() {
        AuthorizationResponse authorizationResponse = AuthorizationResponse.builder()
                .responseMode("query")
                .redirectUri("https://www.idporten.no/")
                .state("sss")
                .code("c")
                .iss("i")
                .build();
        authorizationResponse.addParameter("any", "thing");
        AuditData auditData = authorizationResponse.getAuditData();
        assertAll(
                () -> assertEquals(5, auditData.getAttributes().size()),
                () -> assertEquals("https://www.idporten.no/", auditData.getAttribute("redirect_uri")),
                () -> assertEquals("query", auditData.getAttribute("response_mode")),
                () -> assertEquals("i", auditData.getAttribute("iss")),
                () -> assertEquals("sss", auditData.getAttribute("state")),
                () -> assertEquals("c", auditData.getAttribute("code")),
                () -> assertNull(auditData.getAttribute("any"))
        );
    }

}
