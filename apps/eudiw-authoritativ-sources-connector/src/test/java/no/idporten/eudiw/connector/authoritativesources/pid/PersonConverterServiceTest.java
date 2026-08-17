package no.idporten.eudiw.connector.authoritativesources.pid;

import no.idporten.eudiw.connector.authoritativesources.freg.PersonConverterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PersonConverterServiceTest {

    @DisplayName("verify that age over 18 is calculated correctly")
    @Test
    void testCalcAgeOver() {
        PersonConverterService service = new PersonConverterService();
        assertTrue(service.calcAgeOver("1979-12-11", 18));

        String exactly18Years = getYears(18);
        assertTrue(service.calcAgeOver(exactly18Years, 18));

        String not18Yet = getYears(10);
        assertFalse(service.calcAgeOver(not18Yet, 18));
    }

    private static String getYears(int years) {
        LocalDate fodselsDato = LocalDate.now().minusYears(years);
        return fodselsDato.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @DisplayName("verify that expiry date has correct ISO-8601 date-only format (yyyy-MM-dd)")
    @Test
    void verifyExpiredDateHasCorrectFormat() {
        PersonConverterService service = new PersonConverterService();
        LocalDate expiryDate = service.calcPidExpiryDate();
        assertNotNull(expiryDate);
        // e.g. 2035-09-22T00:00Z
        assertTrue(expiryDate.toString().matches("\\d{4}-\\d{2}-\\d{2}"));
        assertTrue(expiryDate.isAfter(LocalDate.now()));
    }


    @DisplayName("verify that country code NOR converts to NO")
    @Test
    void verifyNORasNationalityReturnsCountryCodeNO() {
        PersonConverterService service = new PersonConverterService();
        // norsk
        List<String> nor = service.getNationalitiesAlpha2(Collections.singletonList("NOR"));
        assertEquals("NO", nor.getFirst());
    }

    @DisplayName("verify that list of country code POL and SWE converts to PL and SE")
    @Test
    void verifyManyNationalityReturnsManyCountryCode() {
        PersonConverterService service = new PersonConverterService();
        List<String> nationalities = new ArrayList<>();
        nationalities.add("POL");
        nationalities.add("SWE");
        List<String> pl = service.getNationalitiesAlpha2(nationalities);
        assertEquals("PL", pl.getFirst());
        assertEquals("SE", pl.getLast());
    }

    @DisplayName("verify that unknown country code return null")
    @Test
    void verifyInvalidNationalityReturnsNull() {
        PersonConverterService service = new PersonConverterService();
        List<String> unknown = service.getNationalitiesAlpha2(Collections.singletonList("XXY"));
        assertTrue(unknown.isEmpty());
    }
}