package no.idporten.eudiw.issuer.claimssource.byob;

import no.idporten.eudiw.issuer.authoritativesources.AuthoritativeSource;
import no.idporten.eudiw.issuer.claimssource.CredentialData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("When using BYOB as claims source")
@ActiveProfiles("junit")
@SpringBootTest
class ByobClaimsSourceTest {

    @Autowired
    ByobClaimsSource claimsSource;

    @DisplayName("when pushing credential data then the same data is returned")
    @Test
    void testPush() {
        CredentialData credentialData = new CredentialData(Map.of("attr1", "value"), "net.eidas2sandkasse:credential-payload");
        CredentialData result = claimsSource.push(null, credentialData);
        assertEquals(credentialData, result);
    }

    @DisplayName("then the authoritative source name is BYOB")
    @Test
    void thenAuthorativeSourceNameIsByob() {
        assertEquals(AuthoritativeSource.BYOB.name(), claimsSource.getAuthorativeSourceName());
    }
}