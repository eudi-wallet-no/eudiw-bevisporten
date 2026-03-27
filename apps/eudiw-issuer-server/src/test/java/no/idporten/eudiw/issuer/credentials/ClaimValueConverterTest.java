package no.idporten.eudiw.issuer.credentials;

import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceFormatException;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedClaimsDescription;
import no.idporten.eudiw.issuer.credentials.types.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static no.idporten.eudiw.issuer.credentials.configurations.ExtendedClaimsDescription.EMPTY_NAMESPACE;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ClaimValueConverter tests")
class ClaimValueConverterTest {

    private final ClaimValueConverter converter = new ClaimValueConverter();

    @DisplayName("when call getFullDateClaim with date as string then input is valid and returns FullDateValue Claim")
    @Test
    void fullDateClaim_from_string_valid() {
        Claim claim = converter.fullDateClaim("birth_date", "2024-03-25");
        assertEquals("birth_date", claim.getPath().getFirst());
        assertInstanceOf(FullDateValue.class, claim.getValue());
        assertEquals(LocalDate.of(2024, 3, 25), ((FullDateValue) claim.getValue()).value());
    }

    @DisplayName("when call getFullDateClaim with invalid format string then throws ClaimsSourceFormatException")
    @Test
    void fullDateClaim_from_string_invalid_format_throws() {
        ClaimsSourceFormatException ex =
                assertThrows(ClaimsSourceFormatException.class,
                        () -> converter.fullDateClaim("birth_date", "25-03-2024"));
        assertTrue(ex.getMessage().contains("Invalid format for claim birth_date from authoritative source"));
    }

    @DisplayName("when call getFullDateClaim with LocalDate then returns FullDateValue Claim")
    @Test
    void fullDateClaim_from_LocalDate() {
        LocalDate date = LocalDate.of(1999, 12, 31);
        Claim claim = converter.fullDateClaim("some_date", date);
        assertEquals("some_date", claim.getPath().getFirst());
        assertInstanceOf(FullDateValue.class, claim.getValue());
        assertEquals(date, ((FullDateValue) claim.getValue()).value());
    }

    @DisplayName("when call getFullDateClaim with null LocalDate then returns FullDateValue Claim with null value")
    @Test
    void fullDateClaim_from_LocalDate_null_allowed() {
        Claim claim = converter.fullDateClaim("null_date", (LocalDate) null);
        assertEquals("null_date", claim.getPath().getFirst());
        assertInstanceOf(FullDateValue.class, claim.getValue());
        assertNull(((FullDateValue) claim.getValue()).value());
    }

    @DisplayName("when call getDateTimeClaim with ZonedDateTime then returns DateTimeValue Claim")
    @Test
    void getDateTimeClaim() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC")).withNano(0);
        Claim claim = converter.getDateTimeClaim("timestamp", now);
        assertEquals("timestamp", claim.getPath().getFirst());
        assertInstanceOf(DateTimeValue.class, claim.getValue());
        assertEquals(now, ((DateTimeValue) claim.getValue()).value());
    }

    @DisplayName("when call getStringClaim with String then returns StringValue Claim")
    @Test
    void getStringClaim() {
        Claim claim = converter.getStringClaim("given_name", "Alice");
        assertEquals("given_name", claim.getPath().getFirst());
        assertInstanceOf(StringValue.class, claim.getValue());
        assertEquals("Alice", ((StringValue) claim.getValue()).value());
    }

    @DisplayName("when call getStringClaim with null then returns StringValue Claim with null value")
    @Test
    void getStringClaim_null_value() {
        Claim claim = converter.getStringClaim("middle_name", null);
        assertEquals("middle_name", claim.getPath().getFirst());
        assertInstanceOf(StringValue.class, claim.getValue());
        assertNull(((StringValue) claim.getValue()).value());
    }

    @DisplayName("when call getNumberClaim with Integer then returns NumberValue Claim")
    @Test
    void getNumberClaim() {
        Claim claim = converter.getNumberClaim("age", 22);
        assertEquals("age", claim.getPath().getFirst());
        assertInstanceOf(NumberValue.class, claim.getValue());
        assertEquals(22, ((NumberValue) claim.getValue()).value());
    }

    @DisplayName("when call getBooleanClaim with true then returns BooleanValue Claim with true value")
    @Test
    void getBooleanClaim_true() {
        Claim claim = converter.getBooleanClaim("active", true);
        assertEquals("active", claim.getPath().getFirst());
        assertInstanceOf(BooleanValue.class, claim.getValue());
        assertTrue(((BooleanValue) claim.getValue()).value());
    }

    @DisplayName("when call getBooleanClaim with null then returns BooleanValue Claim with null value")
    @Test
    void getBooleanClaim_null() {
        Claim claim = converter.getBooleanClaim("flag", null);
        assertEquals("flag", claim.getPath().getFirst());
        assertInstanceOf(BooleanValue.class, claim.getValue());
        assertNull(((BooleanValue) claim.getValue()).value());
    }

    @DisplayName("when call getListClaim with populated list then returns ListValue Claim")
    @Test
    void getListClaim_populated() {
        Claim claim = converter.getListClaim("roles", List.of("USER", "ADMIN"));
        assertEquals("roles", claim.getPath().getFirst());
        assertInstanceOf(ListValue.class, claim.getValue());
        ListValue lv = (ListValue) claim.getValue();
        assertEquals(2, lv.value().size());
        assertTrue(lv.value().stream().allMatch(v -> v instanceof StringValue));
        assertEquals("USER", ((StringValue) lv.value().get(0)).value());
        assertEquals("ADMIN", ((StringValue) lv.value().get(1)).value());
    }

    @DisplayName("when call getListClaim with empty list then returns ListValue Claim with empty value")
    @Test
    void getListClaim_empty() {
        Claim claim = converter.getListClaim("empty_list", List.of());
        assertEquals("empty_list", claim.getPath().getFirst());
        assertInstanceOf(ListValue.class, claim.getValue());
        assertTrue(((ListValue) claim.getValue()).value().isEmpty());
    }

    @DisplayName("when call getMapClaim with populated map then returns MapValue Claim")
    @Test
    void getMapClaim_populated() {
        Map<String, Object> src = Map.of("given_name", "Alice", "family_name", "Doe", "age", 42);
        Claim claim = converter.getMapClaim("name_map", src);
        assertEquals("name_map", claim.getPath().getFirst());
        assertInstanceOf(MapValue.class, claim.getValue());
        MapValue mv = (MapValue) claim.getValue();
        assertEquals(3, mv.value().size());
        assertEquals("Alice", ((StringValue) mv.value().get("given_name")).value());
        assertEquals("Doe", ((StringValue) mv.value().get("family_name")).value());
        assertEquals(42, ((NumberValue) mv.value().get("age")).value());
    }

    @DisplayName("when call getMapClaim with empty map then returns MapValue Claim with empty value")
    @Test
    void getMapClaim_empty() {
        Claim claim = converter.getMapClaim("empty_map", Map.of());
        assertEquals("empty_map", claim.getPath().getFirst());
        assertInstanceOf(MapValue.class, claim.getValue());
        assertTrue(((MapValue) claim.getValue()).value().isEmpty());
    }

    @DisplayName("when call convertClaim with string type then returns StringValue Claim")
    @Test
    void convertClaim_string_type() {
        ExtendedClaimsDescription metadata = new ExtendedClaimsDescription(EMPTY_NAMESPACE, "given_name", ClaimDataType.STRING, null, Map.of("en", "Given Name"), true, null);
        Map<String, Object> storedClaims = Map.of("given_name", "Alice");

        Claim claim = converter.convertClaim(metadata, storedClaims);

        assertEquals("given_name", claim.getPath().getFirst());
        assertInstanceOf(StringValue.class, claim.getValue());
        assertEquals("Alice", ((StringValue) claim.getValue()).value());
    }

    @DisplayName("when call convertClaim with number type then returns NumberValue Claim")
    @Test
    void convertClaim_number_type() {
        ExtendedClaimsDescription metadata = new ExtendedClaimsDescription(EMPTY_NAMESPACE, "age", ClaimDataType.NUMBER, null, Map.of("en", "Age"), true, null);
        Map<String, Object> storedClaims = Map.of("age", 30);

        Claim claim = converter.convertClaim(metadata, storedClaims);

        assertEquals("age", claim.getPath().getFirst());
        assertInstanceOf(NumberValue.class, claim.getValue());
        assertEquals(30, ((NumberValue) claim.getValue()).value());
    }

    @DisplayName("when call convertClaim with boolean type then returns BooleanValue Claim")
    @Test
    void convertClaim_boolean_type() {
        ExtendedClaimsDescription metadata = new ExtendedClaimsDescription(EMPTY_NAMESPACE, "active", ClaimDataType.BOOLEAN, Map.of("en", "Active"), true, null);
        Map<String, Object> storedClaims = Map.of("active", true);

        Claim claim = converter.convertClaim(metadata, storedClaims);

        assertEquals("active", claim.getPath().getFirst());
        assertInstanceOf(BooleanValue.class, claim.getValue());
        assertTrue(((BooleanValue) claim.getValue()).value());
    }

    @DisplayName("when call convertClaim")
    @Nested
    class ConvertClaimsTests {

        @DisplayName("with fulldate type and LocalDate then returns FullDateValue Claim")
        @Test
        void convertClaim_fulldate_type_localdate() {
            ExtendedClaimsDescription metadata = new ExtendedClaimsDescription(EMPTY_NAMESPACE, "birth_date", ClaimDataType.ISO_DATE, Map.of("en", "Birth Date"), true, null);
            LocalDate date = LocalDate.of(1990, 5, 15);
            Map<String, Object> storedClaims = Map.of("birth_date", date);

            Claim claim = converter.convertClaim(metadata, storedClaims);

            assertEquals("birth_date", claim.getPath().getFirst());
            assertInstanceOf(FullDateValue.class, claim.getValue());
            assertEquals(date, ((FullDateValue) claim.getValue()).value());
        }

        @DisplayName("with fulldate type and string then returns FullDateValue Claim")
        @Test
        void convertClaim_fulldate_type_string() {
            ExtendedClaimsDescription metadata = new ExtendedClaimsDescription(EMPTY_NAMESPACE, "birth_date", ClaimDataType.ISO_DATE, Map.of("en", "Birth Date"), true, null);
            Map<String, Object> storedClaims = Map.of("birth_date", "1990-05-15");

            Claim claim = converter.convertClaim(metadata, storedClaims);

            assertEquals("birth_date", claim.getPath().getFirst());
            assertInstanceOf(FullDateValue.class, claim.getValue());
            assertEquals(LocalDate.of(1990, 5, 15), ((FullDateValue) claim.getValue()).value());
        }

        @DisplayName("with datetime type then returns DateTimeValue Claim")
        @Test
        void convertClaim_datetime_type() {
            ExtendedClaimsDescription metadata = new ExtendedClaimsDescription(EMPTY_NAMESPACE, "issued_at", ClaimDataType.ISO_DATE_TIME, Map.of("en", "Issued At"), true, null);
            ZonedDateTime timestamp = ZonedDateTime.now(ZoneId.of("UTC")).withNano(0);
            Map<String, Object> storedClaims = Map.of("issued_at", timestamp);

            Claim claim = converter.convertClaim(metadata, storedClaims);

            assertEquals("issued_at", claim.getPath().getFirst());
            assertInstanceOf(DateTimeValue.class, claim.getValue());
            assertEquals(timestamp, ((DateTimeValue) claim.getValue()).value());
        }

        @DisplayName("with datetime type as string then returns DateTimeValue Claim")
        @Test
        void convertClaim_datetime_type_string() {
            ExtendedClaimsDescription metadata = new ExtendedClaimsDescription(EMPTY_NAMESPACE, "issued_at", ClaimDataType.ISO_DATE_TIME, Map.of("en", "Issued At"), true, null);
            String timestamp = "2024-06-15T12:30:00Z";
            Map<String, Object> storedClaims = Map.of("issued_at", timestamp);

            Claim claim = converter.convertClaim(metadata, storedClaims);

            assertEquals("issued_at", claim.getPath().getFirst());
            assertInstanceOf(DateTimeValue.class, claim.getValue());
        }

        @DisplayName("with binary type then returns BinaryValue Claim")
        @Test
        void convertClaim_binary_type() {
            ExtendedClaimsDescription metadata = new ExtendedClaimsDescription(EMPTY_NAMESPACE, "portrait", ClaimDataType.BINARY, null, Map.of("en", "Portrait"), true, null);
            String base64 = Base64.getEncoder().encodeToString("test".getBytes());
            Map<String, Object> storedClaims = Map.of("portrait", base64);

            Claim claim = converter.convertClaim(metadata, storedClaims);

            assertEquals("portrait", claim.getPath().getFirst());
            assertInstanceOf(BinaryValue.class, claim.getValue());
            assertEquals(base64, ((BinaryValue) claim.getValue()).value());
        }

        @DisplayName("with binary type with mimetype then returns BinaryValue Claim")
        @Test
        void convertClaim_binary_type_with_mimeType() {
            ExtendedClaimsDescription metadata = new ExtendedClaimsDescription(EMPTY_NAMESPACE, "portrait", ClaimDataType.BINARY, "image/png", Map.of("en", "Portrait"), true, null);
            String base64 = Base64.getEncoder().encodeToString("test".getBytes());
            Map<String, Object> storedClaims = Map.of("portrait", base64);

            Claim claim = converter.convertClaim(metadata, storedClaims);

            assertEquals("portrait", claim.getPath().getFirst());
            assertInstanceOf(BinaryValue.class, claim.getValue());
            BinaryValue binaryValue = (BinaryValue) claim.getValue();
            assertEquals(base64, binaryValue.value());
            assertEquals("image/png", binaryValue.mimeType());
        }

        @DisplayName("with unknown type then defaults to StringValue Claim")
        @Test
        void convertClaim_unknown_type_defaults_to_string() {
            ExtendedClaimsDescription metadata = new ExtendedClaimsDescription(EMPTY_NAMESPACE, "custom", null, Map.of("en", "Custom"), true, null);
            Map<String, Object> storedClaims = Map.of("custom", "custom_value");

            Claim claim = converter.convertClaim(metadata, storedClaims);

            assertEquals("custom", claim.getPath().getFirst());
            assertInstanceOf(StringValue.class, claim.getValue());
            assertEquals("custom_value", ((StringValue) claim.getValue()).value());
        }

    }

}