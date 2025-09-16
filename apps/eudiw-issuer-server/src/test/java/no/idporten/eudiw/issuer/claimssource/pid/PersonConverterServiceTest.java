package no.idporten.eudiw.issuer.claimssource.pid;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class PersonConverterServiceTest {

    @DisplayName("verify that age over 18 is calculated correctly")
    @Test
    void testCalcAgeOver18() {
        PersonConverterService service = new PersonConverterService();
        assertTrue(service.calcAgeOver18("1979-12-11"));

        String exactly18Years = getYears(18);
        assertTrue(service.calcAgeOver18(exactly18Years));

        String not18Yet = getYears(10);
        assertFalse(service.calcAgeOver18(not18Yet));
    }

    @NotNull
    private static String getYears(int years) {
        LocalDate fodselsDato = LocalDate.now().minusYears(years);
        return fodselsDato.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }


    @DisplayName("verify that country code NOR converts to NO")
    @Test
    void verifyNORasNationalityReturnsCountryCodeNO() {
        PersonConverterService service = new PersonConverterService();
        // norsk
        String nor = service.getFirstNationalityAlpha2(Collections.singletonList("NOR"));
        assertEquals("NO", nor);
    }

    @DisplayName("verify that country code POL converts to PL")
    @Test
    void verifyPOLasNationalityReturnsCountryCodePL() {
        PersonConverterService service = new PersonConverterService();
        // polsk
        String pl = service.getFirstNationalityAlpha2(Collections.singletonList("POL"));
        assertEquals("PL", pl);
    }

    @DisplayName("verify that unknown country code return null")
    @Test
    void verifyInvalidNationalityReturnsNull() {
        PersonConverterService service = new PersonConverterService();
        String unknown = service.getFirstNationalityAlpha2(Collections.singletonList("XXY"));
        assertNull(unknown);
    }
}