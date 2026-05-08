package no.idporten.eudiw.oauth2.server;

import no.idporten.eudiw.oauth2.server.audit.OpenIDConnectAuditLogger;
import no.idporten.eudiw.oauth2.server.client.ClientMetadata;
import no.idporten.eudiw.oauth2.server.config.OAuth2ServerConfiguration;
import no.idporten.eudiw.oauth2.server.protocol.AuthorizationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("When all hope is lost")
public class PanicErrorResponseTest {

    private OAuth2AuthorizationServerBase authorizationServer;
    private OpenIDConnectAuditLogger auditLogger;

    @BeforeEach
    public void setUp() throws Exception {
        auditLogger = mock(OpenIDConnectAuditLogger.class);
        OAuth2ServerConfiguration serverConfiguration = TestUtils.defaultOAuth2ServerTestConfigurationBuilder()
                .responseMode("form_post")
                .auditLogger(auditLogger)
                .build();
        authorizationServer = new OAuth2AuthorizationServerBase(serverConfiguration);
    }

    @DisplayName("then a signed panic authz error response can be sent to a clients first redirect_uri as a last resort")
    @Test
    void testSendPanicErrorResponse() {
        ClientMetadata clientMetadata = authorizationServer.getConfiguration().findClient("cid");
        AuthorizationResponse authorizationResponse = authorizationServer.panicErrorResponse(clientMetadata, "save_me", "End user lost");
        assertAll(
                () -> assertEquals("https://idporten.no/", authorizationResponse.getRedirectUri()),
                () -> assertEquals("query.jwt", authorizationResponse.getResponseMode()),
                () -> assertNull(authorizationResponse.getState()),
                () -> assertEquals(TestUtils.defaultIssuer(), authorizationResponse.getIss()),
                () -> assertEquals("save_me", authorizationResponse.getError()),
                () -> assertEquals("End user lost", authorizationResponse.getErrorDescription())
        );
    }

}
