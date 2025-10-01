package no.idporten.eudiw.issuer.claimssource.pid;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.digdir.freg.audit.AuditLog;
import no.digdir.freg.domain.json.Folkeregisterfoedsel;
import no.digdir.freg.domain.json.Folkeregisterperson;
import no.digdir.freg.domain.json.Folkeregisterpersonnavn;
import no.digdir.freg.domain.json.Statsborgerskap;
import no.digdir.freg.eventlog.EventLog;
import no.digdir.freg.service.FregResultMapper;
import no.digdir.freg.service.FregService;
import no.digdir.logging.event.EventLogger;
import no.idporten.eudiw.issuer.claimssource.domain.Claim;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.FullDateValue;
import no.idporten.eudiw.issuer.claimssource.domain.StringValue;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceDataNotFoundException;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceInvalidDataException;
import no.idporten.eudiw.issuer.config.ClaimsSourceProperties;
import no.idporten.eudiw.issuer.openid4vci.metadata.ClaimsDescription;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;
import no.idporten.logging.audit.AuditLogger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PIDClaimsSource tests")
@SpringBootTest
@ActiveProfiles("junit")
class PIDClaimsSourceTest {

    public static final int NUMBER_OF_CLAIMS = 10;

    @MockitoBean
    private ClaimsSourceProperties properties;

    private PIDClaimsSource pidClaimsSource;

    @Autowired
    private PersonConverterService personConverterService;

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
        FregService fregService = new FregService(new FregResultMapper(), new AuditLog(auditLogger), new EventLog(eventLogger), new ObjectMapper(), fregIntegration);
        pidClaimsSource = new PIDClaimsSource(fregService, personConverterService);
        pidClaimsSource.init(properties);
    }

    @Test
    @DisplayName("validate that metadata can be retrieved from PIDClaimsSource")
    void verifyMetadata() {
        ClaimsSourceMetadata metadata = pidClaimsSource.getMetadata();
        assertNotNull(metadata);
        assertNotNull(metadata.getClaims());
        assertEquals(NUMBER_OF_CLAIMS, metadata.getClaims().size());
        assertNotNull(metadata.getDisplays().stream()
                .filter(display -> display.getName().contains("Norsk PID"))
                .findFirst()
                .orElse(null));


        // Stikkprøve å finne eit kjent claim
        String expectedName = "Fødselsnummer";
        boolean foundExpectedName = false;
        for (ClaimsDescription desc : metadata.getClaims()) {
            Display d = desc.getDisplays().stream().filter(display -> display.getName().contains(expectedName)).findFirst().orElse(null);
            if (d != null) {
                foundExpectedName = true;
                assertEquals(expectedName, d.getName());
                assertEquals("no", d.getLocale());
                break;
            }
        }
        assertTrue(foundExpectedName, "Did not find claim with name=%s".formatted(expectedName));
    }

    @Test
    @DisplayName("when retriveClaims with a valid accesstoken with fnr as subject should return a valid Person from FREG mapped to Claims")
    void retrieveClaims() {

        String fnr = "12345678901";
        Folkeregisterperson fregPerson = createFolkeregisterperson();
        when(fregIntegration.getFolkeregisterPerson(eq(fnr), anyList())).thenReturn(fregPerson);

        List<Claim> claims = pidClaimsSource.retrieveClaims(createAccessToken(fnr));

        assertNotNull(claims);
        assertEquals(NUMBER_OF_CLAIMS, claims.size());
        verify(fregIntegration).getFolkeregisterPerson(eq(fnr), anyList());

        // test personal_administrative_number is present in claims
        Optional<Claim> fnrClaim = claims.stream().filter(c-> "personal_administrative_number".equals(c.getPath().get(1)))
                .findFirst();
        assertTrue(fnrClaim.isPresent());
        assertInstanceOf(StringValue.class, fnrClaim.get().getValue());
        assertEquals(fnr, fnrClaim.get().getValue().value());

        // test birth_date is present in claims and of type FullDateValue
        Optional<Claim> birthDateClaim = claims.stream().filter(c-> "birth_date".equals(c.getPath().get(1)))
                .findFirst();
        assertTrue(birthDateClaim.isPresent());
        assertInstanceOf(FullDateValue.class, birthDateClaim.get().getValue());
        assertEquals(LocalDate.of(2000, 1, 1), birthDateClaim.get().getValue().value());
    }

    @Test
    @DisplayName("when retriveClaims and person in FREG without statsborgerskap should give ClaimsSourceInvalidDataException")
    void personUtanStatsborgarskapGirException() {

        String fnr = "12345678901";
        Folkeregisterperson fregPerson = createFolkeregisterperson();
        fregPerson.setStatsborgerskap(null);
        when(fregIntegration.getFolkeregisterPerson(eq(fnr), anyList())).thenReturn(fregPerson);

        assertThrows(ClaimsSourceInvalidDataException.class, ()-> pidClaimsSource.retrieveClaims(createAccessToken(fnr)));

    }
    @Test
    @DisplayName("when retriveClaims and person not found in FREG should give ClaimsSourceDataNotFoundException")
    void personIkkjeFunneIFregGirException() {

        String fnr = "12345678901";
        Folkeregisterperson fregPerson = createFolkeregisterperson();
        fregPerson.setStatsborgerskap(null);
        when(fregIntegration.getFolkeregisterPerson(eq(fnr), anyList())).thenThrow(new ClaimsSourceDataNotFoundException("",""));

        assertThrows(ClaimsSourceDataNotFoundException.class, ()-> pidClaimsSource.retrieveClaims(createAccessToken(fnr)));
    }

    @NotNull
    private static JWT createAccessToken(String fnr) {
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder().subject(fnr).build();
        return new PlainJWT(claimSet);
    }

    @NotNull
    private static Folkeregisterperson createFolkeregisterperson() {
        Folkeregisterperson fregPerson = new Folkeregisterperson();
        Folkeregisterpersonnavn navn = new Folkeregisterpersonnavn();
        navn.setEtternavn("Etternavnesen");
        navn.setFornavn("Forenavnesen");
        navn.setErGjeldende(true);
        fregPerson.setNavn(Collections.singletonList(navn));
        Folkeregisterfoedsel fodsel = new Folkeregisterfoedsel();
        fodsel.setFoedeland("NOR");
        fodsel.setFoedselsdato("2000-01-01");
        fodsel.setErGjeldende(true);
        fregPerson.setFoedsel(Collections.singletonList(fodsel));
        Statsborgerskap statsborgerskap = new Statsborgerskap();
        statsborgerskap.setStatsborgerskap("NOR");
        statsborgerskap.setErGjeldende(true);
        fregPerson.setStatsborgerskap(Collections.singletonList(statsborgerskap));
        return fregPerson;
    }

}