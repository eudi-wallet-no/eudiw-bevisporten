package no.idporten.eudiw.oauthserver.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("junit")
@SpringBootTest
class OpenIDConnectConfigurationTest {

    @Autowired
    OpenIDConnectConfiguration openIDConnectConfiguration;

    @Test
    void verifyGrantProperties(){
        assertNotNull(openIDConnectConfiguration.getIssuer());
        assertEquals(1, openIDConnectConfiguration.getScopesSupported().size());
        assertNotNull(openIDConnectConfiguration.getGrantTypesSupported());
        assertTrue(openIDConnectConfiguration.getGrantTypesSupported().contains("authorization_code"));
        assertTrue(openIDConnectConfiguration.getGrantTypesSupported().contains("urn:ietf:params:oauth:grant-type:pre-authorized_code"));
    }

}
