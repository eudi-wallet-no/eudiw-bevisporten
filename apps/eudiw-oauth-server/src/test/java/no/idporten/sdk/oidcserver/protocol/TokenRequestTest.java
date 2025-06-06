package no.idporten.sdk.oidcserver.protocol;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static no.idporten.sdk.oidcserver.util.MultiValuedMapUtils.toMultiValuedMap;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("When handling token requests")
public class TokenRequestTest {

    @Test
    @DisplayName("then core parameters and headers can be parsed")
    public void testCoreParametersAndHeaders() {
        final String authorizationHeader = "Basic foo:bar";
        final String clientId = "foo";
        final String clientSecret = "bar";
        final String code = "ccc";
        final String grantType = "ggg";
        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", clientId);
        parameters.put("client_secret", clientSecret);
        parameters.put("code", code);
        parameters.put("grant_type", grantType);

        TokenRequest tokenRequest = new TokenRequest(Map.of("Authorization", Collections.singletonList(authorizationHeader)), toMultiValuedMap(parameters));
        assertAll(
                () -> assertEquals(authorizationHeader, tokenRequest.getAuthorizationHeader()),
                () -> assertEquals(clientId, tokenRequest.getClientId()),
                () -> assertEquals(clientSecret, tokenRequest.getClientSecret()),
                () -> assertEquals(code, tokenRequest.getCode()),
                () -> assertEquals(grantType, tokenRequest.getGrantType())
        );
    }

    @Test
    @DisplayName("then additional parameters can be parsed")
    public void testAdditionalParameters() {
        final String extra = "xxx";

        TokenRequest tokenRequest = new TokenRequest(Collections.emptyMap(), Map.of("extra", Collections.singletonList(extra)));
        assertEquals(extra, tokenRequest.getParameter("extra"));
    }

    @Test
    @DisplayName("then audit data contains token parameters")
    public void testAuditData() {
        final String clientId = "foo";
        final String clientSecret = "bar";
        final String code = "ccc";
        final String grantType = "ggg";
        final String redirectUri = "https://idporten.no/";
        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", clientId);
        parameters.put("client_secret", clientSecret);
        parameters.put("code", code);
        parameters.put("grant_type", grantType);
        parameters.put("redirect_uri", redirectUri);
        TokenRequest tokenRequest = new TokenRequest(Collections.emptyMap(), toMultiValuedMap(parameters));
        AuditData auditData = tokenRequest.getAuditData();
        assertAll(
                () -> assertEquals(4, auditData.getAttributes().size()),
                () -> assertEquals(clientId, auditData.getAttribute("client_id")),
                () -> assertEquals(code, auditData.getAttribute("code")),
                () -> assertEquals(grantType, auditData.getAttribute("grant_type")),
                () -> assertEquals(redirectUri, auditData.getAttribute("redirect_uri"))
        );
    }

    @Test
    @DisplayName("then audit data does not contain client_secret")
    public void testAuditDataNoSecret() {
        final String clientId = "foo";
        final String clientSecret = "bar";
        Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", clientId);
        parameters.put("client_secret", clientSecret);
        TokenRequest tokenRequest = new TokenRequest(Collections.emptyMap(), toMultiValuedMap(parameters));
        AuditData auditData = tokenRequest.getAuditData();
        assertAll(
                () -> assertEquals(1, auditData.getAttributes().size()),
                () -> assertEquals(clientId, auditData.getAttribute("client_id"))
        );
    }

}