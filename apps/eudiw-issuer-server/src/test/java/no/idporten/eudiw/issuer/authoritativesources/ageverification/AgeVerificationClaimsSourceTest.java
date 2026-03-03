package no.idporten.eudiw.issuer.authoritativesources.ageverification;

import no.digdir.freg.audit.AuditLog;
import no.digdir.freg.domain.json.Folkeregisterperson;
import no.digdir.freg.eventlog.EventLog;
import no.digdir.freg.service.FregResultMapper;
import no.digdir.freg.service.FregService;
import no.digdir.logging.event.EventLogger;
import no.idporten.eudiw.issuer.authoritativesources.pid.FregIntegration;
import no.idporten.eudiw.issuer.authoritativesources.pid.PersonConverterService;
import no.idporten.eudiw.issuer.credentials.types.*;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

import static no.idporten.eudiw.issuer.authoritativesources.pid.FregTestUtils.createFolkeregisterperson;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("junit")
@SpringBootTest
@DisplayName("Test AgeVerificationClaimsSource")
class AgeVerificationClaimsSourceTest {

    public static final int NUMBER_OF_CLAIMS = 2;

    private AgeVerificationClaimsSource aVClaimsSource;

    @Autowired
    private PersonConverterService personConverterService;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean("fregRestClient")
    private RestClient restClient;

    @MockitoBean
    private FregIntegration fregIntegration;

    @MockitoBean
    private AuditLogger auditLogger;

    @MockitoBean
    private EventLogger eventLogger;

    @BeforeEach
    void manuallyConfigureBeans() {
        FregService fregService = new FregService(new FregResultMapper(), new AuditLog(auditLogger), new EventLog(eventLogger), jsonMapper, fregIntegration);
        aVClaimsSource = new AgeVerificationClaimsSource(fregService, personConverterService);
    }

    @Test
    @DisplayName("verify that data can be retrieved from authoritative source and contains BooleanValue claims age_over_18 and age_over_15")
    void pull() {
        String fnr = "12345678901";
        Folkeregisterperson fregPerson = createFolkeregisterperson("2000"); // above 15 and 18 years old
        when(fregIntegration.getFolkeregisterPerson(eq(fnr), anyList())).thenReturn(fregPerson);

        List<Claim> claims = aVClaimsSource.pull(fnr);

        assertNotNull(claims);
        assertEquals(NUMBER_OF_CLAIMS, claims.size());
        verify(fregIntegration).getFolkeregisterPerson(eq(fnr), anyList());

        // test age_over_18 is present in claims and of type BooleanValue of value true
        Optional<Claim> ageOver18Claim = claims.stream().filter(c-> "age_over_18".equals(c.getPath().getLast()))
                .findFirst();
        assertTrue(ageOver18Claim.isPresent());
        assertEquals("eu.europa.ec.av.1", ageOver18Claim.get().getPath().getFirst());
        ClaimValue over18 = ageOver18Claim.get().getValue();
        assertInstanceOf(BooleanValue.class, over18);
        assertTrue(((BooleanValue)over18).value());

        // test age_over_15 is present in claims and of type BooleanValue of value true
        Optional<Claim> ageOver15Claim = claims.stream().filter(c-> "age_over_15".equals(c.getPath().getLast()))
                .findFirst();
        assertTrue(ageOver15Claim.isPresent());
        ClaimValue over15 = ageOver15Claim.get().getValue();
        assertEquals("eu.europa.ec.av.1", ageOver15Claim.get().getPath().getFirst());
        assertInstanceOf(BooleanValue.class, over15);
        assertTrue(((BooleanValue)over15).value());
    }
}