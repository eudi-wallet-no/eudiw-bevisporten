package no.idporten.eudiw.oauth2.server.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("junit")
@SpringBootTest
class OpenID4VCIOAuth2ServerConfigurationTest {

    @Autowired
    OpenID4VCIOAuth2ServerConfiguration openID4VCIOAuth2ServerConfiguration;

    @Test
    void verifyGrantProperties(){
        assertNotNull(openID4VCIOAuth2ServerConfiguration.getIssuer());
        assertEquals(1, openID4VCIOAuth2ServerConfiguration.getScopesSupported().size());
        assertNotNull(openID4VCIOAuth2ServerConfiguration.getGrantTypesSupported());
        assertTrue(openID4VCIOAuth2ServerConfiguration.getGrantTypesSupported().contains("authorization_code"));
        assertTrue(openID4VCIOAuth2ServerConfiguration.getGrantTypesSupported().contains("urn:ietf:params:oauth:grant-type:pre-authorized_code"));
        assertTrue(openID4VCIOAuth2ServerConfiguration.isRequireChallenge());
    }

}
