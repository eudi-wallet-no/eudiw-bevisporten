package no.idporten.sdk.oidcserver.protocol;


import no.idporten.sdk.oidcserver.MockRequest;
import no.idporten.sdk.oidcserver.OAuth2Exception;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When handling pushed authorization requests")
public class PushedAuthorizationRequestTest {

    @Test
    @DisplayName("then core parameters and headers can be parsed")
    public void testCoreParametersAndHeaders() {
        final String authorizationHeader = "Basic foo:bar";
        final String clientId = "foo";
        final String clientSecret = "bar";
        final String state = "ss";
        final String nonce = "nn";
        final String responseType = "code";
        final String responseMode = "form_post";
        MockRequest request = new MockRequest();
        request.addHeader("authorizatioN", authorizationHeader);
        request.addParameter("client_id", clientId);
        request.addParameter("client_secret", clientSecret);
        request.addParameter("scope", "openid  profile ");
        request.addParameter("state", state);
        request.addParameter("nonce", nonce);
        request.addParameter("response_type", responseType);
        request.addParameter("response_mode", responseMode);
        request.addParameter("acr_values", "l3 l4");
        request.addParameter("ui_locales", "nn nb");
        PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
        assertAll(
                () -> assertEquals(authorizationHeader, pushedAuthorizationRequest.getAuthorizationHeader()),
                () -> assertEquals(clientId, pushedAuthorizationRequest.getClientId()),
                () -> assertEquals(clientSecret, pushedAuthorizationRequest.getClientSecret()),
                () -> assertTrue(pushedAuthorizationRequest.getScope().contains("openid")),
                () -> assertTrue(pushedAuthorizationRequest.getScope().contains("profile")),
                () -> assertEquals(state, pushedAuthorizationRequest.getState()),
                () -> assertEquals(nonce, pushedAuthorizationRequest.getNonce()),
                () -> assertEquals(responseType, pushedAuthorizationRequest.getResponseType()),
                () -> assertEquals(responseMode, pushedAuthorizationRequest.getResponseMode()),
                () -> assertTrue(pushedAuthorizationRequest.getAcrValues().contains("l3")),
                () -> assertTrue(pushedAuthorizationRequest.getAcrValues().contains("l4")),
                () -> assertTrue(pushedAuthorizationRequest.getUiLocales().contains("nn")),
                () -> assertTrue(pushedAuthorizationRequest.getUiLocales().contains("nb")),
                () -> assertTrue(pushedAuthorizationRequest.getAuthorizationDetails().isEmpty())
        );
    }

    @Test
    @DisplayName("then empty string delimited parameters are parsed to empty lists")
    public void testEmptyStringDelimitedParameterValues() {
        MockRequest request = new MockRequest();
        PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
        assertAll(
                () -> assertTrue(pushedAuthorizationRequest.getScope().isEmpty()),
                () -> assertTrue(pushedAuthorizationRequest.getAcrValues().isEmpty()),
                () -> assertTrue(pushedAuthorizationRequest.getUiLocales().isEmpty()),
                () -> assertTrue(pushedAuthorizationRequest.getScope().isEmpty())
        );
    }

    @Test
    @DisplayName("then additional parameters can be parsed")
    public void testAdditionalParameters() {
        MockRequest request = new MockRequest();
        request.addParameter("extra", "xxx");
        PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
        assertEquals("xxx", pushedAuthorizationRequest.getParameter("extra"));
    }

    @Test
    @DisplayName("then parsing fails for invalid format for authorization_details")
    public void testInvalidFormatAuthorizationDetails() {
        try {
            MockRequest request = new MockRequest();
            request.addParameter("authorization_details", "xxx");
            new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
            fail();
        } catch (OAuth2Exception e) {
            assertAll(
                    () -> assertEquals("invalid_authorization_details", e.error()),
                    () -> assertEquals(400, e.getHttpStatusCode())
            );
        }
    }

    @Test
    @DisplayName("then valid format for authorization_details can be parsed")
    public void testValidFormatAuthorizationDetails() {
        MockRequest request = new MockRequest();
        request.addParameter("authorization_details", "[{\"type\": \"t1\", \"foo\":\"bar\"}, {\"type\": \"t2\", \"foo\": \"baz\"}]");
        PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
        List<AuthorizationDetail> authorizationDetails = pushedAuthorizationRequest.getAuthorizationDetails();
        assertAll(
                () -> assertEquals(2, authorizationDetails.size()),
                () -> assertEquals("t1", authorizationDetails.get(0).getType()),
                () -> assertEquals("bar", authorizationDetails.get(0).getAttribute("foo")),
                () -> assertEquals("t2", authorizationDetails.get(1).getType()),
                () -> assertEquals("baz", authorizationDetails.get(1).getAttribute("foo"))
        );
    }

    @Test
    @DisplayName("then serialization to cache hides secrets")
    public void testSerializationHidesSecrets(@TempDir File folder) throws Exception {
        final String authorizationHeader = "Basic foo:bar";
        final String clientId = "foo";
        final String clientSecret = "bar";
        MockRequest request = new MockRequest();
        request.addHeader("Authorization", authorizationHeader);
        request.addParameter("client_id", clientId);
        request.addParameter("client_secret", clientSecret);
        PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
        pushedAuthorizationRequest.setLifetimeSeconds(10);

        final File serializedObjectFile = new File(folder, "temp.ser");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(serializedObjectFile))) {
            oos.writeObject(pushedAuthorizationRequest);
        }
        final PushedAuthorizationRequest deserialized;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(serializedObjectFile))) {
            deserialized = (PushedAuthorizationRequest) ois.readObject();
        }
        assertAll(
                () -> assertEquals(pushedAuthorizationRequest, deserialized),
                () -> assertNull(deserialized.getClientSecret()),
                () -> assertNull(deserialized.getAuthorizationHeader()),
                () -> assertEquals(clientId, deserialized.getClientId()),
                () -> assertTrue(deserialized.isValidNow())
        );
    }

    @Test
    @DisplayName("then audit data contains parameters")
    public void testAuditData() {
        final String authorizationHeader = "Basic foo:bar";
        final String clientId = "foo";
        final String clientSecret = "bar";
        final String state = "ss";
        final String nonce = "nn";
        final String responseType = "code";
        final String responseMode = "form_post";
        final String redirectUri = "https://idporten.no/";
        MockRequest request = new MockRequest();
        request.addHeader("Authorization", authorizationHeader);
        request.addParameter("client_id", clientId);
        request.addParameter("redirect_uri", redirectUri);
        request.addParameter("scope", "openid  profile ");
        request.addParameter("state", state);
        request.addParameter("nonce", nonce);
        request.addParameter("response_type", responseType);
        request.addParameter("response_mode", responseMode);
        request.addParameter("acr_values", "l3 l4");
        request.addParameter("ui_locales", "nn nb");
        request.addParameter("authorization_details", "[{\"type\": \"foo\"}]");
        request.addParameter("issuer_state", "is");
        PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
        AuditData auditData = pushedAuthorizationRequest.getAuditData();
        assertAll(
                () -> assertEquals(11, auditData.getAttributes().size()),
                () -> assertEquals(clientId, auditData.getAttribute("client_id")),
                () -> assertEquals(redirectUri, auditData.getAttribute("redirect_uri")),
                () -> assertEquals("openid  profile ", auditData.getAttribute("scope")),
                () -> assertEquals(state, auditData.getAttribute("state")),
                () -> assertEquals(nonce, auditData.getAttribute("nonce")),
                () -> assertEquals(responseType, auditData.getAttribute("response_type")),
                () -> assertEquals(responseMode, auditData.getAttribute("response_mode")),
                () -> assertEquals("l3 l4", auditData.getAttribute("acr_values")),
                () -> assertEquals("nn nb", auditData.getAttribute("ui_locales")),
                () -> assertNotNull(auditData.getAttribute("authorization_details")),
                () -> assertEquals("is", auditData.getAttribute("issuer_state"))
        );
    }

    @Test
    @DisplayName("then audit data does not contain client_secret")
    public void testAuditDataNoSecret() {
        final String clientId = "foo";
        final String clientSecret = "bar";
        MockRequest request = new MockRequest();
        request.addParameter("client_id", clientId);
        request.addParameter("client_secret", clientSecret);
        PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(request.getHeaders(), request.getParameters());
        AuditData auditData = pushedAuthorizationRequest.getAuditData();
        assertAll(
                () -> assertEquals(1, auditData.getAttributes().size()),
                () -> assertEquals(clientId, auditData.getAttribute("client_id"))
        );
    }

}
