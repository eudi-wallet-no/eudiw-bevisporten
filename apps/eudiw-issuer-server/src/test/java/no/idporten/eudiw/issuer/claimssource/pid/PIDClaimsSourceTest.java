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

import java.util.Collections;
import java.util.List;

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
        int metadataClaimsIncludingMapDescriptions = NUMBER_OF_CLAIMS + 2;
        assertEquals(metadataClaimsIncludingMapDescriptions, metadata.getClaims().size());
        assertNotNull(metadata.getDisplays().stream()
                .filter(display -> display.getName().contains("Norsk PID"))
                .findFirst()
                .orElse(null));


        // Stikkprøve å finne eit kjendt claim
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
    @DisplayName("verify that claims can be retrieved from PIDClaimsSource for an access token with fnr as subject")
    void retrieveClaims() {

        String fnr = "12345678901";
        Folkeregisterperson fregPerson = createFolkeregisterperson();
        when(fregIntegration.getFolkeregisterPerson(eq(fnr), anyList())).thenReturn(fregPerson);

        List<Claim> claims = pidClaimsSource.retrieveClaims(createAccessToken(fnr));

        assertNotNull(claims);
        assertEquals(NUMBER_OF_CLAIMS, claims.size());
        // TODO testing of actual claim values
        verify(fregIntegration).getFolkeregisterPerson(eq(fnr), anyList());
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