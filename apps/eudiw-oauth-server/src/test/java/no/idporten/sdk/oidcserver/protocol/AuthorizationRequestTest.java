package no.idporten.sdk.oidcserver.protocol;

import no.idporten.sdk.oidcserver.MockRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("When handling authorization requests")
public class AuthorizationRequestTest {

    @Test
    @DisplayName("then core parameters can be parsed")
    public void testCoreParameters() {
        final String requestUri = "urn:test:foo";
        MockRequest request = new MockRequest();
        request.addParameter("request_uri", requestUri);
        AuthorizationRequest authorizationRequest = new AuthorizationRequest(request.getHeaders(), request.getParameters());
        assertEquals(requestUri, authorizationRequest.getRequestUri());
    }

    @Test
    @DisplayName("then audit data contains authorization request parameters")
    public void testAuditAuthroizationRequestParameters() {
        final String requestUri = "urn:test:foo";
        MockRequest request = new MockRequest();
        request.addParameter("request_uri", requestUri);
        request.addParameter("foo", "bar");
        AuthorizationRequest authorizationRequest = new AuthorizationRequest(request.getHeaders(), request.getParameters());
        AuditData auditData = authorizationRequest.getAuditData();
        assertEquals(requestUri, auditData.getAttribute("request_uri"));
        assertNull(auditData.getAttribute("foo"));
    }

    @Test
    @DisplayName("then audit data contains user agent")
    public void testAuditUserAgentHeader() {
        MockRequest request = new MockRequest();
        request.addHeader("User-Agent", "junit-ua");
        AuthorizationRequest authorizationRequest = new AuthorizationRequest(request.getHeaders(), request.getParameters());
        AuditData auditData = authorizationRequest.getAuditData();
        assertEquals("junit-ua", auditData.getAttribute("user_agent"));
    }

    @Test
    @DisplayName("then user agent is chopped after 256 characters")
    public void testShortenLongUserAgent() {
        String longUa = String.join("", Collections.nCopies(300, "X"));
        MockRequest request = new MockRequest();
        request.addHeader("User-Agent", longUa);
        AuthorizationRequest authorizationRequest = new AuthorizationRequest(request.getHeaders(), request.getParameters());
        AuditData auditData = authorizationRequest.getAuditData();
        assertEquals(256, String.valueOf(auditData.getAttribute("user_agent")).length());
    }

}
