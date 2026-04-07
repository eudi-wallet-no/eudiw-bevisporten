package no.idporten.eudiw.connector.authoritativesources.pid;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.digdir.freg.domain.json.Folkeregisterfoedsel;
import no.digdir.freg.domain.json.Folkeregisterperson;
import no.digdir.freg.domain.json.Folkeregisterpersonnavn;
import no.digdir.freg.domain.json.Statsborgerskap;
import no.idporten.eudiw.connector.authoritativesources.TestData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;
import no.idporten.eudiw.connector.authoritativesources.exceptions.AuthoritativeSourceException;
import no.idporten.eudiw.connector.authoritativesources.exceptions.AuthoritativeSourceDataNotFoundException;
import no.idporten.eudiw.connector.authoritativesources.freg.FregIntegration;
import no.idporten.lib.maskinporten.client.JwtGrantTokenInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.MockServerRestClientCustomizer;
import org.springframework.boot.test.context.SpringBootTest;
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

@SpringBootTest
@ActiveProfiles("junit")
class FregConfigurationTest {


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
        Subject subject = TestData.getValidSubject();
        FregPerson person = createFregPerson();
        String personString = jsonMapper.writeValueAsString(person);

        customizer.getServer().expect(requestTo("v1/personer/%s?part=person-basis".formatted(subject.identifier())))
                .andRespond(withSuccess(personString, MediaType.APPLICATION_JSON));

        assertNotNull(client);

        // call client
        Folkeregisterperson folkeregisterPerson = client.getFolkeregisterPerson(subject.identifier(), Collections.singletonList("person-basis"));

        // verify
        assertNotNull(folkeregisterPerson);
        assertEquals("Etternavnesen", folkeregisterPerson.getNavn().getEtternavn());
        assertEquals("Forenavnesen", folkeregisterPerson.getNavn().getFornavn());
        assertEquals("2000-01-01", folkeregisterPerson.getFoedsel().getFoedselsdato());
        assertEquals(2, folkeregisterPerson.getStatsborgerskap().size());
        assertEquals("NOR", folkeregisterPerson.getStatsborgerskap().stream().filter(Statsborgerskap::getErGjeldende).findFirst().get().getStatsborgerskap());
    }

    @DisplayName("should return 400 and throw exception")
    @Test
    void fregCallReturns400ThenThrowsException() {
        Subject subject = TestData.getValidSubject();

        customizer.getServer().expect(requestTo("v1/personer/%s?part=person-basis".formatted(subject.identifier())))
                .andRespond(withBadRequest());

        assertNotNull(client);

        assertThrows(AuthoritativeSourceException.class, () ->
                client.getFolkeregisterPerson(subject.identifier(), Collections.singletonList("person-basis"))
        );

    }

    @DisplayName("should return 404 and throw NotFoundException")
    @Test
    void fregCallReturns404ThenThrowsNotFoundException() {
        Subject subject = TestData.getValidSubject();

        customizer.getServer().expect(requestTo("v1/personer/%s?part=person-basis".formatted(subject.identifier())))
                .andRespond(withResourceNotFound());

        assertNotNull(client);

        assertThrows(AuthoritativeSourceDataNotFoundException.class, () ->
                client.getFolkeregisterPerson(subject.identifier(), Collections.singletonList("person-basis"))
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

    private static Statsborgerskap createStatsborgarskap(String statsborgerskap1, boolean erGjeldende) {
        Statsborgerskap statsborgerskap = new Statsborgerskap();
        statsborgerskap.setStatsborgerskap(statsborgerskap1);
        statsborgerskap.setErGjeldende(erGjeldende);
        return statsborgerskap;
    }

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