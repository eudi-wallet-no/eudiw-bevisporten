package no.idporten.eudiw.issuer.authoritativesources.pid;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.digdir.freg.domain.json.Folkeregisterfoedsel;
import no.digdir.freg.domain.json.Folkeregisterperson;
import no.digdir.freg.domain.json.Folkeregisterpersonnavn;
import no.digdir.freg.domain.json.Statsborgerskap;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceDataNotFoundException;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceException;
import no.idporten.lib.maskinporten.client.JwtGrantTokenInterceptor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.MockServerRestClientCustomizer;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RestClientTest(components = {FregIntegration.class, FregProperties.class, FregConfiguration.class})
@ActiveProfiles("junit")
class FregConfigurationTest {

    private static final String fnr = "12345678910";

    private FregIntegration client;

    @MockitoBean
    private JwtGrantTokenInterceptor maskinportenInterceptor;

    @Autowired
    private JsonMapper jsonMapper;

    private MockServerRestClientCustomizer customizer;

    @BeforeEach
    public void setUp() {
        customizer = new MockServerRestClientCustomizer();
        RestClient.Builder builder = RestClient.builder();
        customizer.customize(builder);
        client = new FregIntegration(builder.build());
    }

    @DisplayName("Freg client is configured correctly and can retrieve person")
    @Test
    void folkeregisterPersonIsReturnedWithAllAttributesNeeded() throws JsonProcessingException {

        // setup mock response from Freg
        FregPerson person = createFregPerson();
        String personString = jsonMapper.writeValueAsString(person);

        customizer.getServer().expect(requestTo("v1/personer/%s?part=person-basis".formatted(fnr)))
                .andRespond(withSuccess(personString, MediaType.APPLICATION_JSON));

        assertNotNull(client);

        // call client
        Folkeregisterperson folkeregisterPerson = client.getFolkeregisterPerson(fnr, Collections.singletonList("person-basis"));

        // verify
        assertNotNull(folkeregisterPerson);
        assertEquals("Etternavnesen", folkeregisterPerson.getNavn().getEtternavn());
        assertEquals("Forenavnesen", folkeregisterPerson.getNavn().getFornavn());
        assertEquals("2000-01-01", folkeregisterPerson.getFoedsel().getFoedselsdato());
        assertEquals(2, folkeregisterPerson.getStatsborgerskap().size());
        assertEquals("NOR", folkeregisterPerson.getStatsborgerskap().stream().filter(Statsborgerskap::getErGjeldende).findFirst().get().getStatsborgerskap());
    }

    @DisplayName("")
    @Test
    void fregCallReturns400ThenThrowsClaimSourceException() {

        customizer.getServer().expect(requestTo("v1/personer/%s?part=person-basis".formatted(fnr)))
                .andRespond(withBadRequest());

        assertNotNull(client);

        assertThrows(ClaimsSourceException.class, () ->
                client.getFolkeregisterPerson(fnr, Collections.singletonList("person-basis"))
        );

    }

    @DisplayName("")
    @Test
    void fregCallReturns404ThenThrowsClaimSourceDataNotFoundException() {

        customizer.getServer().expect(requestTo("v1/personer/%s?part=person-basis".formatted(fnr)))
                .andRespond(withResourceNotFound());

        assertNotNull(client);

        assertThrows(ClaimsSourceDataNotFoundException.class, () ->
                client.getFolkeregisterPerson(fnr, Collections.singletonList("person-basis"))
        );

    }

    protected FregPerson createFregPerson() {
        FregPerson fregPerson = new FregPerson();

        List<Folkeregisterpersonnavn> navnList = new ArrayList<>();
        navnList.add(createFolkeregisterpersonnavn("Etternavnesen", true));
        navnList.add(createFolkeregisterpersonnavn("GamaltEtternamn", false));
        fregPerson.setNavn(navnList);

        Folkeregisterfoedsel fodsel = new Folkeregisterfoedsel();
        fodsel.setFoedeland("NOR");
        fodsel.setFoedselsdato("2000-01-01");
        fodsel.setErGjeldende(true);
        fregPerson.setFoedsel(Collections.singletonList(fodsel));

        List<Statsborgerskap> statsborgerskap = new ArrayList<>();
        statsborgerskap.add(createStatsborgarskap("NOR", true));
        statsborgerskap.add(createStatsborgarskap("POL", false));
        fregPerson.setStatsborgerskap(statsborgerskap);

        return fregPerson;
    }

    @NotNull
    private static Statsborgerskap createStatsborgarskap(String statsborgerskap1, boolean erGjeldende) {
        Statsborgerskap statsborgerskap = new Statsborgerskap();
        statsborgerskap.setStatsborgerskap(statsborgerskap1);
        statsborgerskap.setErGjeldende(erGjeldende);
        return statsborgerskap;
    }

    @NotNull
    private static Folkeregisterpersonnavn createFolkeregisterpersonnavn(String etternavn, boolean isGjeldende) {

        Folkeregisterpersonnavn navnGjeldende = new Folkeregisterpersonnavn();
        navnGjeldende.setEtternavn(etternavn);
        navnGjeldende.setFornavn("Forenavnesen");
        navnGjeldende.setMellomnavn("Mellomnavn");
        navnGjeldende.setForkortetNavn("For Etter");
        navnGjeldende.setErGjeldende(isGjeldende);
        return navnGjeldende;

    }

}