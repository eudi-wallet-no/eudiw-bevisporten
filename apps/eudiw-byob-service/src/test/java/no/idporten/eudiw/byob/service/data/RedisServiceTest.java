package no.idporten.eudiw.byob.service.data;

import no.idporten.eudiw.byob.service.model.data.CredentialConfigurationData;
import no.idporten.eudiw.byob.service.model.data.CredentialMetadataData;
import no.idporten.eudiw.byob.service.model.data.ExampleCredentialDataData;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Redis Service Test")
@ActiveProfiles("dev")
@SpringBootTest
@Disabled // this test runs now against actual redis on localhost, integration test to verify configuration. Todo write new unit-tests.
class RedisServiceTest {

    @Autowired
    private RedisConfig config;

    @Autowired
    private RedisService redisService;

    @Test
    public void testSaveBevistypeToRedis() {
        CredentialConfigurationData bevisType1 = createCredentialConfigurationData("test-vct:1");
        redisService.addBevisType(bevisType1);
    }

    private static CredentialConfigurationData createCredentialConfigurationData(String vct) {
        return new CredentialConfigurationData("test-vct-cred-id", vct, "dc+sd-jwt", List.of(new ExampleCredentialDataData("attr1","mine")), new CredentialMetadataData(List.of(), List.of()));
    }

    @Test
    public void testGetAllFromRedis() {
        CredentialConfigurationData bevisType1 = createCredentialConfigurationData("test-vct:2");
        redisService.addBevisType(bevisType1);
        List<CredentialConfigurationData> all = redisService.getAll();
        assertNotNull(all);
        assertFalse(all.isEmpty());
    }

    @Test
    public void deleteFromRedis() {
        CredentialConfigurationData bevisType1 = createCredentialConfigurationData("test-vct-to-delete");
        redisService.addBevisType(bevisType1);
        assertNotNull(redisService.getBevisType(bevisType1.vct()));
        redisService.delete(bevisType1.vct());
        assertNull(redisService.getBevisType(bevisType1.vct()));
    }

    @Disabled
    @Test
    public void deleteAllFromRedis() {
        CredentialConfigurationData bevisType1 = createCredentialConfigurationData("test-vct-to-delete-1");
        redisService.addBevisType(bevisType1);
        CredentialConfigurationData bevisType2 = createCredentialConfigurationData("test-vct-to-delete-2");
        redisService.addBevisType(bevisType2);
        assertNotNull(redisService.getBevisType(bevisType1.vct()));
        assertNotNull(redisService.getBevisType(bevisType2.vct()));
        redisService.deleteAll();
        assertNull(redisService.getBevisType(bevisType1.vct()));
        assertNull(redisService.getBevisType(bevisType2.vct()));
    }
}