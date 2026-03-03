package no.idporten.eudiw.issuer.authoritativesources.pid;

import no.digdir.freg.audit.AuditLog;
import no.digdir.freg.domain.json.Folkeregisterperson;
import no.digdir.freg.eventlog.EventLog;
import no.digdir.freg.service.FregResultMapper;
import no.digdir.freg.service.FregService;
import no.digdir.logging.event.EventLogger;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceDataNotFoundException;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceInvalidDataException;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static no.idporten.eudiw.issuer.authoritativesources.pid.FregTestUtils.createFolkeregisterperson;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PIDClaimsSource tests")
@SpringBootTest
@ActiveProfiles("junit")
class PIDClaimsSourceTest {

    public static final int NUMBER_OF_CLAIMS = 9;

    private PIDClaimsSource pidClaimsSource;

    @Autowired
    private PersonConverterService personConverterService;

    @MockitoBean("fregRestClient")
    private RestClient restClient;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private FregIntegration fregIntegration;

    @MockitoBean
    private AuditLogger auditLogger;

    @MockitoBean
    private EventLogger eventLogger;


    @BeforeEach
    void manuallyConfigureBeans() {
        FregService fregService = new FregService(new FregResultMapper(), new AuditLog(auditLogger), new EventLog(eventLogger), jsonMapper, fregIntegration);
        pidClaimsSource = new PIDMdocClaimsSource(fregService, personConverterService);
    }

    @Test
    @DisplayName("then data can be pulled from authoritative source with a valid accesstoken with fnr as subject should return a valid Person from FREG mapped to Claims for mdoc format")
    void pullMdoc() {

        String fnr = "12345678901";
        Folkeregisterperson fregPerson = createFolkeregisterperson();
        when(fregIntegration.getFolkeregisterPerson(eq(fnr), anyList())).thenReturn(fregPerson);

        List<Claim> claims = pidClaimsSource.pull(fnr);

        assertNotNull(claims);
        assertEquals(NUMBER_OF_CLAIMS, claims.size());
        assertTrue(claims.stream().anyMatch(c-> "family_name".equals(c.getPath().getLast())));
        assertTrue(claims.stream().anyMatch(c-> "given_name".equals(c.getPath().getLast())));
        assertTrue(claims.stream().anyMatch(c-> "birth_date".equals(c.getPath().getLast())));
        assertTrue(claims.stream().anyMatch(c-> "place_of_birth".equals(c.getPath().getLast())));
        assertTrue(claims.stream().anyMatch(c-> "nationality".equals(c.getPath().getLast())));

        verify(fregIntegration).getFolkeregisterPerson(eq(fnr), anyList());

        // test personal_administrative_number is present in claims
        Optional<Claim> fnrClaim = claims.stream().filter(c-> "personal_administrative_number".equals(c.getPath().getLast())).findFirst();
        assertTrue(fnrClaim.isPresent());
        assertInstanceOf(StringValue.class, fnrClaim.get().getValue());
        assertEquals(fnr, fnrClaim.get().getValue().value());

        // test birth_date is present in claims and of type FullDateValue
        Optional<Claim> birthDateClaim = claims.stream().filter(c-> List.of("eu.europa.ec.eudi.pid.1", "birth_date").equals(c.getPath())).findFirst();
        assertTrue(birthDateClaim.isPresent());
        assertInstanceOf(FullDateValue.class, birthDateClaim.get().getValue());
        assertEquals(LocalDate.of(2000, 1, 1), birthDateClaim.get().getValue().value());
    }

    @Test
    @DisplayName("then attribute names can be constructed for SD-JWT VC format")
    void pullSDJwtVC() {
        FregService fregService = new FregService(new FregResultMapper(), new AuditLog(auditLogger), new EventLog(eventLogger), jsonMapper, fregIntegration);
        PIDSDJwtClaimsSource pidCs = new PIDSDJwtClaimsSource(fregService, personConverterService);
        String fnr = "12345678901";
        Folkeregisterperson fregPerson = createFolkeregisterperson();
        when(fregIntegration.getFolkeregisterPerson(eq(fnr), anyList())).thenReturn(fregPerson);

        List<Claim> claims = pidCs.pull(fnr);
        assertAll(
                () -> assertNotNull(claims),
                () -> assertEquals(NUMBER_OF_CLAIMS, claims.size()),
                () -> assertTrue(claims.stream().anyMatch(c -> "family_name".equals(c.getPath().getFirst()))),
                () -> assertTrue(claims.stream().anyMatch(c -> "given_name".equals(c.getPath().getFirst()))),
                () -> assertTrue(claims.stream().anyMatch(c -> "birthdate".equals(c.getPath().getFirst()))),
                () -> assertTrue(claims.stream().anyMatch(c -> "place_of_birth".equals(c.getPath().getFirst()))),
                () -> assertTrue(claims.stream().anyMatch(c -> "nationalities".equals(c.getPath().getFirst())))
        );
    }

    @Test
    @DisplayName("then data can be pulled from authoritative source and person in FREG without statsborgerskap should give ClaimsSourceInvalidDataException")
    void personUtanStatsborgarskapGirException() {

        String fnr = "12345678901";
        Folkeregisterperson fregPerson = createFolkeregisterperson();
        fregPerson.setStatsborgerskap(null);
        when(fregIntegration.getFolkeregisterPerson(eq(fnr), anyList())).thenReturn(fregPerson);

        assertThrows(ClaimsSourceInvalidDataException.class, ()-> pidClaimsSource.pull(fnr));

    }
    @Test
    @DisplayName("then data can be pulled from authoritative source and person not found in FREG should give ClaimsSourceDataNotFoundException")
    void personIkkjeFunneIFregGirException() {

        String fnr = "12345678901";
        Folkeregisterperson fregPerson = createFolkeregisterperson();
        fregPerson.setStatsborgerskap(null);
        when(fregIntegration.getFolkeregisterPerson(eq(fnr), anyList())).thenThrow(new ClaimsSourceDataNotFoundException("",""));

        assertThrows(ClaimsSourceDataNotFoundException.class, ()-> pidClaimsSource.pull(fnr));
    }



}