package no.idporten.eudiw.oauth2.server.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import no.idporten.eudiw.oauth2.server.OpenID4VCIAuthorizationServer;
import no.idporten.eudiw.oauth2.server.protocol.*;
import no.idporten.eudiw.oauth2.server.proxy.OIDCProxyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("When handling authorization proxy requests")
class AuthorizationProxyEndpointControllerTest {

    @Mock
    private OpenID4VCIAuthorizationServer authorizationServer;
    @Mock
    private OIDCProxyService oidcProxyService;
    @Mock
    private PushedAuthorizationRequest pushedAuthorizationRequest;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpSession session;
    @Test
    @DisplayName("then /authorize redirects to upstream OIDC")
    void shouldRedirectToOidc() {
        AuthorizationProxyEndpointController controller = new AuthorizationProxyEndpointController(authorizationServer, oidcProxyService);
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        com.nimbusds.oauth2.sdk.AuthorizationRequest upstreamRequest = mock(com.nimbusds.oauth2.sdk.AuthorizationRequest.class);

        when(authorizationServer.process(any(AuthorizationRequest.class))).thenReturn(pushedAuthorizationRequest);
        when(oidcProxyService.createAuthorizationRequest(eq(pushedAuthorizationRequest), any())).thenReturn(upstreamRequest);
        when(upstreamRequest.toURI()).thenReturn(URI.create("https://login.test.idporten.no/authorize?request_uri=urn:1"));

        String response = controller.authorize(headers, parameters, request, session);

        assertEquals("redirect:https://login.test.idporten.no/authorize?request_uri=urn:1", response);
        verify(session).setAttribute(AuthorizationProxyEndpointController.SESSION_PUSHED_AUTHORIZATION_REQUEST, pushedAuthorizationRequest);
        verify(oidcProxyService).createAuthorizationRequest(eq(pushedAuthorizationRequest), any());
    }
}
