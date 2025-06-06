package no.idporten.sdk.oidcserver;

import no.idporten.sdk.oidcserver.audit.OpenIDConnectAuditLogger;
import no.idporten.sdk.oidcserver.client.ClientMetadata;
import no.idporten.sdk.oidcserver.config.OpenIDConnectSdkConfiguration;
import no.idporten.sdk.oidcserver.protocol.AuthorizationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("When all hope is lost")
public class PanicErrorResponseTest {

    private OpenIDConnectIntegrationBase openIDConnectSdk;
    private OpenIDConnectAuditLogger auditLogger;

    @BeforeEach
    public void setUp() throws Exception {
        auditLogger = mock(OpenIDConnectAuditLogger.class);
        OpenIDConnectSdkConfiguration sdkConfiguration = TestUtils.defaultSdkTestConfigurationBuilder()
                .responseMode("form_post")
                .auditLogger(auditLogger)
                .build();
        openIDConnectSdk = new OpenIDConnectIntegrationBase(sdkConfiguration);
    }

    @DisplayName("then a signed panic authz error response can be sent to a clients first redirect_uri as a last resort")
    @Test
    void testSendPanicErrorResponse() {
        ClientMetadata clientMetadata = openIDConnectSdk.getSDKConfiguration().findClient("cid");
        AuthorizationResponse authorizationResponse = openIDConnectSdk.panicErrorResponse(clientMetadata, "save_me", "End user lost");
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
