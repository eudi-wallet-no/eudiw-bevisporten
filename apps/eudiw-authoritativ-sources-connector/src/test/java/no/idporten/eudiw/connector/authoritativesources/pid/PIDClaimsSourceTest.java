package no.idporten.eudiw.connector.authoritativesources.pid;

import no.digdir.freg.domain.json.Folkeregisterperson;
import no.digdir.freg.domain.json.Statsborgerskap;
import no.idporten.eudiw.connector.authoritativesources.TestData;
import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;
import no.idporten.eudiw.connector.authoritativesources.exceptions.ClaimsSourceInvalidDataException;
import no.idporten.eudiw.connector.authoritativesources.freg.FregIntegration;
import no.idporten.eudiw.connector.authoritativesources.freg.PersonConverterService;
import no.idporten.eudiw.connector.authoritativesources.freg.PidMdocCredentialDataSource;
import no.idporten.eudiw.connector.authoritativesources.freg.PidSdJwtCredentialDataSource;
import no.idporten.eudiw.connector.authoritativesources.freg.pidfields.MdocFieldNames;
import no.idporten.eudiw.connector.authoritativesources.freg.pidfields.SdJwtFieldNames;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static no.idporten.eudiw.connector.authoritativesources.pid.FregTestUtils.createFolkeregisterperson;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DisplayName("PIDClaimsSource tests")
@SpringBootTest
@ActiveProfiles("junit")
class PIDClaimsSourceTest {

    public static final int NUMBER_OF_CLAIMS = 9;

    @Autowired
    private PidMdocCredentialDataSource pidMdocCredentialDataSource;

    @Autowired
    private PidSdJwtCredentialDataSource sdJwtCredentialDataSource;

    @Autowired
    private PersonConverterService converterService;

    @MockitoBean("fregRestClient")
    private RestClient restClient;

    @MockitoBean
    private FregIntegration fregIntegration;


    @Test
    @DisplayName("then data can be pulled from authoritative source with a valid accesstoken with fnr as subject should return a valid Person from FREG mapped to CredentialData for mdoc format")
    void retrievePidMdoc() {
        Subject subject = TestData.getValidSubject();
        Folkeregisterperson fregPerson = createFolkeregisterperson();
        when(fregIntegration.getFolkeregisterPerson(eq(subject.identifier()), anyList())).thenReturn(fregPerson);

        CredentialData cd = pidMdocCredentialDataSource.retrieveCredentialData(subject);

        MdocFieldNames fieldNames = MdocFieldNames.getInstance();

        assertNotNull(cd);
        assertEquals(NUMBER_OF_CLAIMS, cd.size());
        assertEquals(subject.identifier(), cd.get(fieldNames.getPersonalAdministrativeNumber()));
        assertEquals(fregPerson.getFoedsel().getFoedselsdato(), cd.get(fieldNames.getBirthDate()));

        Object placeOfBirth = cd.get(fieldNames.getPlaceOfBirth());
        assertNotNull(placeOfBirth);

        if (placeOfBirth instanceof Map<?, ?> map && map.containsKey("country")) {
            assertEquals(converterService.getNationalityAlpha2(fregPerson.getFoedsel().getFoedeland()), map.get("country"));
        } else {
            fail("Expected a Map with key 'country' but got: " + placeOfBirth);
        }

        Object nationalities = cd.get(fieldNames.getNationality());
        if (nationalities instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof String) {
            List<String> actualNationalities = list.stream()
                    .map(Object::toString)
                    .toList();
            List<String> expectedNationalities = fregPerson.getStatsborgerskap().stream()
                    .map(Statsborgerskap::getStatsborgerskap)
                    .toList();
            assertEquals(expectedNationalities.size(), actualNationalities.size(), "Unexpected number of nationalities");
            assertEquals(converterService.getNationalitiesAlpha2(expectedNationalities), actualNationalities, "Nationalities do not match expected values");
        } else {
            fail("Expected a non-empty List<String> but got: " + nationalities);
        }

        assertEquals(fregPerson.getNavn().getEtternavn(), cd.get(fieldNames.getFamilyName()));

        String givenName = (String) cd.get(fieldNames.getGivenName());
        assertTrue(givenName.contains(fregPerson.getNavn().getFornavn()));
    }

    @Test
    @DisplayName("then data can be pulled from authoritative source with a valid accesstoken with fnr as subject should return a valid Person from FREG mapped to CredentialData for sw-jwt format")
    void retrievePidSdJwt() {
        Subject subject = TestData.getValidSubject();
        Folkeregisterperson fregPerson = createFolkeregisterperson();
        when(fregIntegration.getFolkeregisterPerson(eq(subject.identifier()), anyList())).thenReturn(fregPerson);

        CredentialData cd = sdJwtCredentialDataSource.retrieveCredentialData(subject);

        SdJwtFieldNames fieldNames = SdJwtFieldNames.getInstance();

        assertNotNull(cd);
        assertEquals(NUMBER_OF_CLAIMS, cd.size());
        assertEquals(subject.identifier(), cd.get(fieldNames.getPersonalAdministrativeNumber()));
        assertEquals(fregPerson.getFoedsel().getFoedselsdato(), cd.get(fieldNames.getBirthDate()));

        Object placeOfBirth = cd.get(fieldNames.getPlaceOfBirth());
        assertNotNull(placeOfBirth);

        if (placeOfBirth instanceof Map<?, ?> map && map.containsKey("country")) {
            assertEquals(converterService.getNationalityAlpha2(fregPerson.getFoedsel().getFoedeland()), map.get("country"));
        } else {
            fail("Expected a Map with key 'country' but got: " + placeOfBirth);
        }


        Object nationalities = cd.get(fieldNames.getNationality());
        if (nationalities instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof String) {
            List<String> actualNationalities = list.stream()
                    .map(Object::toString)
                    .toList();
            List<String> expectedNationalities = fregPerson.getStatsborgerskap().stream()
                    .map(Statsborgerskap::getStatsborgerskap)
                    .toList();
            assertEquals(expectedNationalities.size(), actualNationalities.size(), "Unexpected number of nationalities");
            assertEquals(converterService.getNationalitiesAlpha2(expectedNationalities), actualNationalities, "Nationalities do not match expected values");
        } else {
            fail("Expected a non-empty List<String> but got: " + nationalities);
        }

        assertEquals(fregPerson.getNavn().getEtternavn(), cd.get(fieldNames.getFamilyName()));

        String givenName = (String) cd.get(fieldNames.getGivenName());
        assertTrue(givenName.contains(fregPerson.getNavn().getFornavn()));
    }

    @Test
    @DisplayName("then data can be pulled from authoritative source and person in FREG without statsborgerskap should give ClaimsSourceInvalidDataException")
    void personUtanStatsborgarskapGirException() {

        Subject subject = TestData.getValidSubject();
        Folkeregisterperson fregPerson = createFolkeregisterperson();
        fregPerson.setStatsborgerskap(null);
        when(fregIntegration.getFolkeregisterPerson(eq(subject.identifier()), anyList())).thenReturn(fregPerson);

        assertThrows(ClaimsSourceInvalidDataException.class, () -> pidMdocCredentialDataSource.retrieveCredentialData(subject));
    }
}