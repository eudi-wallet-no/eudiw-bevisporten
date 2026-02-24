package no.idporten.eudiw.issuer.claimssource.byob;

import no.idporten.eudiw.issuer.authoritativesources.AuthoritativeSource;
import no.idporten.eudiw.issuer.claimssource.CredentialData;
import no.idporten.eudiw.issuer.claimssource.CredentialMetadataContext;
import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicCredentialConfiguration;
import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicCredentialMetadata;
import no.idporten.eudiw.issuer.credentials.types.DocumentMetadata;
import no.idporten.eudiw.issuer.credentials.types.DocumentMetadata.Display;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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

    @DisplayName("then getDocumentMetadata returns valid DocumentMetadata for valid credential_configuration_id")
    @Test
    void getDocumentMetadataForValidCredentialConfigurationIdReturnsValidDocumentMetadata() {
        String credentialConfigurationId = "net.eidas2sandkasse:cred-config-id";
        String claimName = "name-claim";
        String vct = "net.eidas2sandkasse:credential-type";
        DynamicCredentialConfiguration cc = createDynamicCredentialConfiguration(credentialConfigurationId, claimName, vct);
        when(integration.retrieve(eq(vct))).thenReturn(cc);
        DocumentMetadata documentMetadata = claimsSource.getDocumentMetadata(new CredentialMetadataContext(credentialConfigurationId, vct, null));
        assertNotNull(documentMetadata);
        assertNotNull(documentMetadata.displays());
        assertNotNull(documentMetadata.claims());
        assertNotNull(documentMetadata.findClaimMetadata(claimName));
        assertEquals(claimName, documentMetadata.findClaimMetadata(claimName).name());
    }

    @NotNull
    private static DynamicCredentialConfiguration createDynamicCredentialConfiguration(String credentialConfigurationId, String claimName, String credentialType) {
        DynamicCredentialMetadata credentialMetadata = new DynamicCredentialMetadata(List.of(new Display("no", "bevis1")), List.of(new DynamicClaimMetadata(claimName, List.of(new Display("no", "navn")), true, ".*")));
        return new DynamicCredentialConfiguration(credentialConfigurationId, "eudiw:any:scope", credentialType, credentialMetadata, "sd-jwt");
    }

    @DisplayName("then getDocumentMetadata returns null for non-existing credential_configuration_id")
    @Test
    void getDocumentMetadataForNonExistingCredentialConfigurationIdReturnEmptyDocumentMetadata() {
        String credentialConfigurationId = "net.eidas2sandkasse:cred-config-id";
        when(integration.retrieveAll()).thenReturn(null);
        assertNull(claimsSource.getDocumentMetadata(new CredentialMetadataContext(credentialConfigurationId, null, null)));
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