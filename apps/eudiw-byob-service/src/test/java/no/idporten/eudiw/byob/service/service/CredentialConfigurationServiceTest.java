package no.idporten.eudiw.byob.service.service;

import no.idporten.eudiw.byob.service.data.RedisService;
import no.idporten.eudiw.byob.service.model.CredentialConfiguration;
import no.idporten.eudiw.byob.service.model.CredentialConfigurations;
import no.idporten.eudiw.byob.service.model.data.CredentialConfigurationData;
import no.idporten.eudiw.byob.service.model.web.ClaimsRequestResource;
import no.idporten.eudiw.byob.service.model.web.CredentialConfigurationRequestResource;
import no.idporten.eudiw.byob.service.model.web.CredentialMetadataRequestResource;
import no.idporten.eudiw.byob.service.model.web.DisplayRequestResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static no.idporten.eudiw.byob.service.service.CredentialConfigurationContext.PUBLIC_CREDENTIAL_TYPE_PREFIX;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CredentialConfigurationService Test")
@ExtendWith(MockitoExtension.class)
class CredentialConfigurationServiceTest {

    @Mock
    private RedisService redisService;

    @InjectMocks
    private CredentialConfigurationService credentialConfigurationService;

    @DisplayName("when creating a new CredentialConfiguration then it is stored in Redis")
    @Test
    void create() {
        CredentialConfigurationRequestResource credentialConfigurationRequestResource = createCredentialConfigurationRequestResource("vct-example");
        CredentialConfiguration credentialConfiguration = credentialConfigurationService.create(credentialConfigurationRequestResource, CredentialConfigurationContext.forPublic());
        assertAll(
                () -> assertEquals(PUBLIC_CREDENTIAL_TYPE_PREFIX + credentialConfigurationRequestResource.credentialType(), credentialConfiguration.credentialType()),
                () -> assertNotNull(credentialConfiguration.credentialConfigurationId()),
                () -> assertTrue(credentialConfiguration.credentialConfigurationId().startsWith(PUBLIC_CREDENTIAL_TYPE_PREFIX)),
                () -> assertNotNull(credentialConfiguration.credentialMetadata()),
                () -> assertNotNull(credentialConfiguration.credentialMetadata().display()),
                () -> assertFalse(credentialConfiguration.credentialMetadata().display().isEmpty()),
                () -> assertEquals(credentialConfigurationRequestResource.credentialMetadata().display().getFirst().name(), credentialConfiguration.credentialMetadata().display().getFirst().name()),
                () -> verify(redisService).addBevisType(any(CredentialConfigurationData.class))
        );
    }

    private static CredentialConfigurationRequestResource createCredentialConfigurationRequestResource(String credentialType) {
        DisplayRequestResource bevisName = createDisplay("Bevis Name");
        DisplayRequestResource claimName = createDisplay("Claim Name");
        return new CredentialConfigurationRequestResource(credentialType, "dc+sd-jwt", "eudiw:foo", null, new CredentialMetadataRequestResource(List.of(bevisName), List.of(new ClaimsRequestResource("path", null, null,true, List.of(claimName)))));
    }

    private static DisplayRequestResource createDisplay(String displayName) {
        return new DisplayRequestResource(displayName, "no", null, null);
    }

    @Test
    void getAllEntriesWhenNothingInRedisReturnsEmpty() {

        CredentialConfigurations all = credentialConfigurationService.getAllEntries(CredentialConfigurationContext.forAdmin());
        assertAll(
                () -> assertNotNull(all),
                () -> assertNotNull(all.credentialConfigurations()),
                () -> assertTrue(all.credentialConfigurations().isEmpty()),
                () -> verify(redisService).getAll()
        );
    }
    @Test
    void getAllEntriesWhenDataInRedisReturnsData() {

        List<CredentialConfigurationData> credentialConfigurationData = List.of(
                getCredentialConfigurationData("cc-id-1", "vct-1"),
                getCredentialConfigurationData("cc-id-2", "vct-2")
        );
        when(redisService.getAll()).thenReturn(credentialConfigurationData);
        CredentialConfigurations all = credentialConfigurationService.getAllEntries(CredentialConfigurationContext.forAdmin());
        assertAll(
                () -> assertNotNull(all),
                () -> assertNotNull(all.credentialConfigurations()),
                () -> assertEquals(credentialConfigurationData.size(),all.credentialConfigurations().size()),
                () -> verify(redisService).getAll()
        );
    }

    @Test
    void getCredentialConfiguration() {
        String credentialType = "net.eidas2sandkasse:some-vct";
        when(redisService.getBevisType(eq(credentialType)))
                .thenReturn(getCredentialConfigurationData("net.eidas2sandkasse:some-cc-id_mso_mdoc", credentialType));
        CredentialConfiguration credentialConfiguration = credentialConfigurationService.getCredentialConfiguration(credentialType, CredentialConfigurationContext.forPublic());
        assertAll(
                () -> assertNotNull(credentialConfiguration),
                () -> assertEquals(credentialType, credentialConfiguration.credentialType()),
                () -> verify(redisService).getBevisType(eq(credentialType))
        );
    }

    private static CredentialConfigurationData getCredentialConfigurationData(String credentialConfigurationId, String credentialType) {
        return new CredentialConfigurationData(credentialConfigurationId, credentialType, "dc+sd-jwt", "eudiw:foo", null, null);
    }

    @Test
    void searchCredentialConfiguration() {
        String credentialConfigurationId = "my-credential-configuration-id";
        when(redisService.getBevisTypeByCredentialConfiguration(eq(credentialConfigurationId)))
                .thenReturn(getCredentialConfigurationData(credentialConfigurationId, "some-vct"));
        CredentialConfiguration credentialConfiguration = credentialConfigurationService.searchCredentialConfiguration(credentialConfigurationId);
        assertAll(
                () -> assertNotNull(credentialConfiguration),
                () -> assertEquals(credentialConfigurationId, credentialConfiguration.credentialConfigurationId()),
                () -> verify(redisService).getBevisTypeByCredentialConfiguration(eq(credentialConfigurationId))
        );
    }
}