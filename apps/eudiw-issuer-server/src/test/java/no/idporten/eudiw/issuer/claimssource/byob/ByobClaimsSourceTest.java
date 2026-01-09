package no.idporten.eudiw.issuer.claimssource.byob;

import no.idporten.eudiw.issuer.claimssource.AuthoritativeSource;
import no.idporten.eudiw.issuer.claimssource.CredentialData;
import no.idporten.eudiw.issuer.claimssource.CredentialMetadataContext;
import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicCredentialConfiguration;
import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicCredentialConfigurations;
import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicCredentialMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata.Display;
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

    @DisplayName("then the claims source is loaded and initialized")
    @Test
    void testClaimsSourceInitialized() {
        assertAll(
                () -> assertNotNull(claimsSource),
                () -> assertNotNull(claimsSource.getProperties())
        );
    }

    @DisplayName("then supports returns true for credential_configuration_id with existing credential configuration")
    @Test
    void whenCallSupportsWithExisitingCredentialConfigurationIdThenReturnTrue() {
        String credentialConfigurationId = "cred-config-id";
        String claimName = "name-claim";
        String vct = "credential-type";
        DynamicCredentialConfigurations cc = createDynamicCredentialConfigurations(credentialConfigurationId, claimName, vct);
        when(integration.retrieveAll()).thenReturn(cc);
        boolean supports = claimsSource.supports(vct);
        assertTrue(supports);

    }

    @DisplayName("then supports returns false when credential_configuration_id does not exist")
    @Test
    void whenCallSupportsWithNonExisitingCredentialConfigurationIdThenReturnFalse() {
        String credentialConfigurationId = "cred-config-id";
        String claimName = "name-claim";
        String vct = "credential-type";
        DynamicCredentialConfigurations cc = createDynamicCredentialConfigurations(credentialConfigurationId, claimName, vct);
        when(integration.retrieveAll()).thenReturn(cc);
        boolean supports = claimsSource.supports("non-existing-credential-type");
        assertFalse(supports);
    }

    @DisplayName("then supports returns false when no credential configurations are available")
    @Test
    void whenCallSupportsWithNoCredentialConfigurationsThenReturnFalse() {
        boolean supports = claimsSource.supports("non-existing-credential-type");
        assertFalse(supports);
    }

    @DisplayName("then getDocumentMetadata returns valid DocumentMetadata for valid credential_configuration_id")
    @Test
    void getDocumentMetadataForValidCredentialConfigurationIdReturnsValidDocumentMetadata() {
        String credentialConfigurationId = "cred-config-id";
        String claimName = "name-claim";
        DynamicCredentialConfigurations cc = createDynamicCredentialConfigurations(credentialConfigurationId, claimName, "credential-type");
        when(integration.retrieveAll()).thenReturn(cc);
        DocumentMetadata documentMetadata = claimsSource.getDocumentMetadata(new CredentialMetadataContext(credentialConfigurationId, null, null));
        assertNotNull(documentMetadata);
        assertNotNull(documentMetadata.displays());
        assertNotNull(documentMetadata.claims());
        assertNotNull(documentMetadata.findClaimMetadata(claimName));
        assertEquals(claimName, documentMetadata.findClaimMetadata(claimName).name());
    }

    @NotNull
    private static DynamicCredentialConfigurations createDynamicCredentialConfigurations(String credentialConfigurationId, String claimName, String vct) {
        DynamicCredentialMetadata credentialMetadata = new DynamicCredentialMetadata(List.of(new Display("no", "bevis1")), List.of(new DynamicClaimMetadata(claimName, List.of(new Display("no", "navn")), true, ".*")));
        DynamicCredentialConfigurations cc = new DynamicCredentialConfigurations(List.of(new DynamicCredentialConfiguration(credentialConfigurationId, vct, credentialMetadata, "sd-jwt")));
        return cc;
    }

    @DisplayName("then getDocumentMetadata returns null for non-existing credential_configuration_id")
    @Test
    void getDocumentMetadataForNonExistingCredentialConfigurationIdReturnEmptyDocumentMetadata() {
        String credentialConfigurationId = "cred-config-id";
        when(integration.retrieveAll()).thenReturn(null);
        DocumentMetadata documentMetadata = claimsSource.getDocumentMetadata(new CredentialMetadataContext(credentialConfigurationId, null, null));
        assertNull(documentMetadata);
    }


    @DisplayName("when pushing credential data then the same data is returned")
    @Test
    void testPush() {
        CredentialData credentialData = new CredentialData(Map.of("attr1", "value"), "credential-payload");
        CredentialData result = claimsSource.push(null, credentialData);
        assertEquals(credentialData, result);
    }

    @DisplayName("then the authoritative source name is BYOB")
    @Test
    void thenAuthorativeSourceNameIsByob() {
        assertEquals(AuthoritativeSource.BYOB.name(), claimsSource.getAuthorativeSourceName());
    }
}