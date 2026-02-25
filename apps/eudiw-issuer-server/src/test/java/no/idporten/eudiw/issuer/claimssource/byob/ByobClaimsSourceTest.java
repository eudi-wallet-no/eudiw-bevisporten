package no.idporten.eudiw.issuer.claimssource.byob;

import no.idporten.eudiw.issuer.authoritativesources.AuthoritativeSource;
import no.idporten.eudiw.issuer.claimssource.CredentialData;
import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicCredentialConfiguration;
import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicCredentialMetadata;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("When using BYOB as claims source")
@ActiveProfiles("junit")
@SpringBootTest
class ByobClaimsSourceTest {

    @Autowired
    ByobClaimsSource claimsSource;

    @Autowired
    DynamicCredentialConfigurationService dynamicCredentialConfigurationService;

    @MockitoBean
    ByobServiceIntegration integration;

    @NotNull
    private static DynamicCredentialConfiguration createDynamicCredentialConfiguration(String credentialConfigurationId, String claimName, String credentialType) {
        DynamicCredentialMetadata credentialMetadata = new DynamicCredentialMetadata(List.of(new Display("no", "bevis1")), List.of(new DynamicClaimMetadata(claimName, List.of(new Display("no", "navn")), true, ".*")));
        return new DynamicCredentialConfiguration(credentialConfigurationId, "eudiw:any:scope", credentialType, credentialMetadata, "sd-jwt");
    }

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