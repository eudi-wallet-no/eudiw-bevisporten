package no.idporten.eudiw.oauth2.server.proxy;

import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import no.idporten.eudiw.oauth2.server.protocol.PushedAuthorizationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("When building authorization request for upstream OIDC")
class OIDCProxyServiceTest {

    private static final String HEADLESS_SYNTHETIC_PID = "16903349844";
    private static final String HEADLESS_ACR = "idporten-loa-substantial";
    private static final String HEADLESS_LOGIN_HINT = "testid:16903349844_idporten-loa-substantial";

    @Test
    @DisplayName("then login_hint is attached for configured headless test client")
    void shouldAttachLoginHintForConfiguredHeadlessClient() {
        OIDCProxyProperties properties = createProperties();
        OIDCProxyProperties.HeadlessLoginProperties headlessLogin = new OIDCProxyProperties.HeadlessLoginProperties();
        headlessLogin.setEnabled(true);
        headlessLogin.setSyntheticPid(HEADLESS_SYNTHETIC_PID);
        headlessLogin.setAcr(HEADLESS_ACR);
        headlessLogin.setClientIds(List.of("wallet-client"));
        properties.setHeadlessLogin(headlessLogin);
        CapturingOIDCProxyService service = new CapturingOIDCProxyService(properties);

        service.createAuthorizationRequest(createPushedAuthorizationRequest("wallet-client"), ProtocolVerifiers.forLogin());

        assertEquals(List.of(HEADLESS_LOGIN_HINT), service.capturedRequest.getCustomParameter("login_hint"));
    }

    @Test
    @DisplayName("then login_hint is not attached for non-configured clients")
    void shouldNotAttachLoginHintWhenClientNotConfigured() {
        OIDCProxyProperties properties = createProperties();
        OIDCProxyProperties.HeadlessLoginProperties headlessLogin = new OIDCProxyProperties.HeadlessLoginProperties();
        headlessLogin.setEnabled(true);
        headlessLogin.setSyntheticPid(HEADLESS_SYNTHETIC_PID);
        headlessLogin.setAcr(HEADLESS_ACR);
        headlessLogin.setClientIds(List.of("wallet-client"));
        properties.setHeadlessLogin(headlessLogin);
        CapturingOIDCProxyService service = new CapturingOIDCProxyService(properties);

        service.createAuthorizationRequest(createPushedAuthorizationRequest("other-client"), ProtocolVerifiers.forLogin());

        assertNull(service.capturedRequest.getCustomParameter("login_hint"));
    }

    private static OIDCProxyProperties createProperties() {
        OIDCProxyProperties properties = new OIDCProxyProperties();
        properties.setRedirectUri(URI.create("https://oauth-server.example/callback"));
        properties.setOidcIssuer(new OIDCIssuerProperties(
                new Issuer("https://test.idporten.no"),
                URI.create("https://login.test.idporten.no/authorize"),
                URI.create("https://test.idporten.no/par"),
                URI.create("https://test.idporten.no/token"),
                URI.create("https://test.idporten.no/jwks.json")));
        OIDCClientProperties clientProperties = new OIDCClientProperties();
        clientProperties.setClientID(new ClientID("oidc-client"));
        clientProperties.setClientAuthenticationMethod(ClientAuthenticationMethod.PRIVATE_KEY_JWT);
        properties.setOidcClient(clientProperties);
        return properties;
    }

    private static PushedAuthorizationRequest createPushedAuthorizationRequest(String clientId) {
        return new PushedAuthorizationRequest(
                Map.of(),
                Map.of(
                        "client_id", List.of(clientId),
                        "redirect_uri", List.of("https://wallet.example/callback"),
                        "response_type", List.of("code"),
                        "scope", List.of("openid"),
                        "code_challenge", List.of("abc"),
                        "code_challenge_method", List.of("S256")));
    }

    private static class CapturingOIDCProxyService extends OIDCProxyService {

        private AuthenticationRequest capturedRequest;

        private CapturingOIDCProxyService(OIDCProxyProperties oidcProxyProperties) {
            super(oidcProxyProperties);
        }

        @Override
        protected AuthenticationRequest pushAuthorizationRequest(AuthenticationRequest authenticationRequest) {
            this.capturedRequest = authenticationRequest;
            return authenticationRequest;
        }
    }
}
