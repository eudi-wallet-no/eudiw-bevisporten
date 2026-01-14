package no.idporten.eudiw.byob.service;

import no.idporten.eudiw.byob.service.data.TestRedisConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = TestRedisConfiguration.class)
@ActiveProfiles("junit")
class ByobServiceApplicationTest {

    @Test
    void contextLoads() {
    }
}