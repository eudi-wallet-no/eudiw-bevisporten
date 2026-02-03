package no.idporten.eudiw.issuer.claimssource.byob;

import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicCredentialConfiguration;
import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicCredentialConfigurations;
import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicCredentialMetadata;
import no.idporten.eudiw.issuer.credentials.types.DocumentMetadata;
import no.idporten.eudiw.issuer.openid4vci.CredentialIssuerServerGeneratorService;
import no.idporten.eudiw.issuer.openid4vci.metadata.CredentialConfiguration;
import no.idporten.eudiw.issuer.openid4vci.metadata.CredentialIssuerMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;

import static no.idporten.eudiw.issuer.claimssource.byob.ByobClaimsSource.DYNAMIC_CREDENTIAL_CONFIGURATION_PREFIX;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@SpringBootTest
@ActiveProfiles("schedule")
@DisplayName("When ByobMetadataScheduler runs updateIssuerMetadata")
class ByobMetadataSchedulerTest {

    @MockitoSpyBean
    private CredentialIssuerMetadata credentialIssuerMetadata;

    @MockitoSpyBean
    private CredentialIssuerServerGeneratorService generatorService;

    @Autowired
    private ByobMetadataScheduler byobMetadataScheduler;

    @MockitoBean
    private ByobServiceIntegration byobServiceIntegration;

    @BeforeEach
    void setUp() {
        // Clear any existing credential configurations in Metadata before each test
        credentialIssuerMetadata.getCredentialConfigurations().clear();
    }

    @Test
    @DisplayName("then updates CredentialIssuerMetadata with 1 new BYOB credential configurations, then new run with first delete and new number 2 added returns only last in metadata, then run with new 3 and still existing 2 returns both in metadata")
    void updateIssuerMetadataWhenByobIsDeletedAndAdded() {

        // 0. Startup also initialized CredentialIssuerMetadata, but with 0 byob configured yet
        final int byobRetrieveAllCallsStartup = 2;

        assertAll(
                () -> assertNotNull(credentialIssuerMetadata),
                () -> assertNotNull(credentialIssuerMetadata.getCredentialConfigurations()),
                () -> assertTrue(credentialIssuerMetadata.getCredentialConfigurations().isEmpty()),
                () -> verify(credentialIssuerMetadata, never()).addCredentialConfigurations(anyString(), any(CredentialConfiguration.class)),
                () -> verify(byobServiceIntegration, times(byobRetrieveAllCallsStartup)).retrieveAll()
        );

        // 1. run with one (1) added
        String vct1 = "test-config-1";
        String credentialConfigurationId1 = DYNAMIC_CREDENTIAL_CONFIGURATION_PREFIX + vct1 + "_sd_jwt";
        DynamicCredentialConfigurations credentialConfigs1 = createCredentialConfigs(vct1, credentialConfigurationId1);
        when(byobServiceIntegration.retrieveAll()).thenReturn(credentialConfigs1);


        byobMetadataScheduler.updateIssuerMetadata();


        assertAll(
                () -> assertNotNull(credentialIssuerMetadata),
                () -> assertNotNull(credentialIssuerMetadata.getCredentialConfigurations()),
                () -> assertEquals(1, credentialIssuerMetadata.getCredentialConfigurations().size()),
                () -> verify(credentialIssuerMetadata).addCredentialConfigurations(eq(credentialConfigurationId1), any(CredentialConfiguration.class)),
                () -> verify(generatorService, times(1)).getByobCredentialConfigurations(),
                () -> verify(byobServiceIntegration, times(byobRetrieveAllCallsStartup + 1)).retrieveAll()
        );


        // 2. run with last one (1) deleted and one new added (2)
        String vct2 = "test-config-2";
        String credentialConfigurationId2 = DYNAMIC_CREDENTIAL_CONFIGURATION_PREFIX + vct2 + "_sd_jwt";
        DynamicCredentialConfiguration credentialConfig2 = createOneCredentialConfig(vct2, credentialConfigurationId2);
        DynamicCredentialConfigurations credentialConfigs2 = DynamicCredentialConfigurations.builder().credentialConfigurations(List.of(credentialConfig2)).build();
        when(byobServiceIntegration.retrieveAll()).thenReturn(credentialConfigs2); // does not return the previous one, so it is deleted correctly by another api call outside scope of this test

        byobMetadataScheduler.updateIssuerMetadata();

        assertAll(
                () -> assertNotNull(credentialIssuerMetadata),
                () -> assertNotNull(credentialIssuerMetadata.getCredentialConfigurations()),
                () -> assertEquals(1, credentialIssuerMetadata.getCredentialConfigurations().size()),
                () -> assertNotNull(credentialIssuerMetadata.getCredentialConfigurations().get(credentialConfigurationId2)),
                () -> assertNull(credentialIssuerMetadata.getCredentialConfigurations().get(credentialConfigurationId1)),
                () -> verify(credentialIssuerMetadata).addCredentialConfigurations(eq(credentialConfigurationId2), any(CredentialConfiguration.class)),
                () -> verify(generatorService, times(2)).getByobCredentialConfigurations(),
                () -> verify(byobServiceIntegration, times(byobRetrieveAllCallsStartup + 2)).retrieveAll()
        );


        // 3. run with one existing (2) from previous run and one new added (3)
        String vct3 = "test-config-3";
        String credentialConfigurationId3 = DYNAMIC_CREDENTIAL_CONFIGURATION_PREFIX + vct3 + "_sd_jwt";
        DynamicCredentialConfiguration credentialConfig3 = createOneCredentialConfig(vct3, credentialConfigurationId3);
        DynamicCredentialConfigurations credentialConfigsWith2 = DynamicCredentialConfigurations.builder().credentialConfigurations(List.of(credentialConfig2, credentialConfig3)).build();
        when(byobServiceIntegration.retrieveAll()).thenReturn(credentialConfigsWith2);

        byobMetadataScheduler.updateIssuerMetadata();

        assertAll(
                () -> assertNotNull(credentialIssuerMetadata),
                () -> assertNotNull(credentialIssuerMetadata.getCredentialConfigurations()),
                () -> assertEquals(2, credentialIssuerMetadata.getCredentialConfigurations().size()),
                () -> assertNotNull(credentialIssuerMetadata.getCredentialConfigurations().get(credentialConfigurationId2)),
                () -> assertNotNull(credentialIssuerMetadata.getCredentialConfigurations().get(credentialConfigurationId3)),
                () -> assertNull(credentialIssuerMetadata.getCredentialConfigurations().get(credentialConfigurationId1)),
                () -> verify(credentialIssuerMetadata, times(2)).addCredentialConfigurations(eq(credentialConfigurationId2), any(CredentialConfiguration.class)),
                () -> verify(credentialIssuerMetadata).addCredentialConfigurations(eq(credentialConfigurationId3), any(CredentialConfiguration.class)),
                () -> verify(generatorService, times(3)).getByobCredentialConfigurations(),
                () -> verify(byobServiceIntegration, times(byobRetrieveAllCallsStartup + 3)).retrieveAll()
        );

    }

    private static DynamicCredentialConfigurations createCredentialConfigs(String vct, String credentialConfigurationId) {
        return DynamicCredentialConfigurations.builder().credentialConfigurations(List.of(createOneCredentialConfig(vct, credentialConfigurationId))).build();
    }

    private static DynamicCredentialConfiguration createOneCredentialConfig(String vct, String credentialConfigurationId) {
        return DynamicCredentialConfiguration.builder()
                .credentialConfigurationId(credentialConfigurationId)
                .format("sd_jwt")
                .vct(vct)
                .credentialMetadata(DynamicCredentialMetadata.builder()
                        .display(List.of(new DocumentMetadata.Display("no", "A test credential from BYOB", "black", null)))
                        .claims(List.of(new DynamicClaimMetadata("claim1", List.of(new DocumentMetadata.Display("no", "Claim 1", null, null)), true, null)))
                        .build())
                .build();
    }
}