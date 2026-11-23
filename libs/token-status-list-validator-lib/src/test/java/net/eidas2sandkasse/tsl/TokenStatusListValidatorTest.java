package net.eidas2sandkasse.tsl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static net.eidas2sandkasse.tsl.TestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("When validating against a token status list")
class TokenStatusListValidatorTest {

    private static final Duration AFTER_ISSUANCE = Duration.ofMinutes(1);
    private static final Duration BEYOND_SKEW = TokenStatusListValidatorConfig.defaults().clockSkew().plusSeconds(1);
    private static final Duration WITHIN_SKEW = TokenStatusListValidatorConfig.defaults().clockSkew().minusSeconds(1);

    @Test
    @DisplayName("and the JWT status at idx is 0 then true is expected")
    void jwtStatusZeroIsValid() {
        var endpoint = new MockStatusListEndpoint().serve(PT_URI, JWT, statusList("pt-pid-mdoc.jwt"));

        var validator = validator(endpoint, PT_ISSUED_AT.plus(AFTER_ISSUANCE));

        assertTrue(validator.validate(URI.create(PT_URI), PT_IDX_VALID));
        assertEquals(0, validator.retrieveStatus(URI.create(PT_URI), PT_IDX_VALID));
    }

    @Test
    @DisplayName("and the JWT status at idx is 1 then false is expected")
    void jwtStatusOneIsNotValid() {
        var endpoint = new MockStatusListEndpoint().serve(PT_URI, JWT, statusList("pt-pid-mdoc.jwt"));

        var validator = validator(endpoint, PT_ISSUED_AT.plus(AFTER_ISSUANCE));

        assertFalse(validator.validate(URI.create(PT_URI), PT_IDX_REVOKED));
        assertEquals(1, validator.retrieveStatus(URI.create(PT_URI), PT_IDX_REVOKED));
    }

    @Test
    @DisplayName("and the CWT has x5chain in the protected header then the status is resolved")
    void cwtWithProtectedX5Chain() {
        var endpoint = new MockStatusListEndpoint().serve(PT_URI, CWT, statusList("pt-pid-mdoc.cwt"));
        var validator = validator(endpoint, PT_ISSUED_AT.plus(AFTER_ISSUANCE));

        assertTrue(validator.validate(URI.create(TestData.PT_URI), PT_IDX_VALID));
        assertEquals(0, validator.retrieveStatus(URI.create(TestData.PT_URI), PT_IDX_VALID));
        assertFalse(validator.validate(URI.create(TestData.PT_URI), PT_IDX_REVOKED));
        assertEquals(1, validator.retrieveStatus(URI.create(TestData.PT_URI), PT_IDX_REVOKED));
    }

    @Test
    @DisplayName("and the CWT has x5chain in the unprotected header then the status is resolved")
    void cwtWithUnprotectedX5Chain() {
        var endpoint = new MockStatusListEndpoint().serve(UA_URI, CWT, statusList("ua-pid-mdoc.cwt"));
        var validator = validator(endpoint, UA_ISSUED_AT.plus(AFTER_ISSUANCE));

        assertTrue(validator.validate(URI.create(TestData.UA_URI), UA_IDX_VALID));
        assertEquals(0, validator.retrieveStatus(URI.create(TestData.UA_URI), UA_IDX_VALID));
        assertFalse(validator.validate(URI.create(TestData.UA_URI), UA_IDX_REVOKED));
        assertEquals(1, validator.retrieveStatus(URI.create(TestData.UA_URI), UA_IDX_REVOKED));
    }

    @Test
    @DisplayName("and the CWT with x5chain in the unprotected header is tampered with then an exception is expected")
    void tamperedCwtWithUnprotectedX5Chain() {
        byte[] cwt = statusList("ua-pid-mdoc.cwt");
        cwt[cwt.length - 1] ^= 0x01;
        var endpoint = new MockStatusListEndpoint().serve(UA_URI, CWT, cwt);

        assertThrows(TokenStatusListException.class,
                () -> validator(endpoint, UA_ISSUED_AT.plus(AFTER_ISSUANCE)).validate(URI.create(TestData.UA_URI), UA_IDX_VALID));
    }

    @Test
    @DisplayName("and sub does not match the uri then an exception is expected")
    void subjectMismatch() {
        var endpoint = new MockStatusListEndpoint().serve(OTHER_URI, JWT, statusList("pt-pid-mdoc.jwt"));

        assertThrows(TokenStatusListException.class,
                () -> validator(endpoint, PT_ISSUED_AT.plus(AFTER_ISSUANCE)).validate(URI.create(TestData.OTHER_URI), PT_IDX_VALID));
    }

    @Test
    @DisplayName("and the JWT is expired then an exception is expected")
    void expiredJwt() {
        var endpoint = new MockStatusListEndpoint().serve(PT_URI, JWT, statusList("pt-pid-mdoc.jwt"));

        assertThrows(TokenStatusListException.class,
                () -> validator(endpoint, PT_EXPIRES_AT.plus(BEYOND_SKEW)).validate(URI.create(TestData.PT_URI), PT_IDX_VALID));
    }

    @Test
    @DisplayName("and the CWT is expired then an exception is expected")
    void expiredCwt() {
        var endpoint = new MockStatusListEndpoint().serve(PT_URI, CWT, statusList("pt-pid-mdoc.cwt"));

        assertThrows(TokenStatusListException.class,
                () -> validator(endpoint, PT_EXPIRES_AT.plus(BEYOND_SKEW)).validate(URI.create(TestData.PT_URI), PT_IDX_VALID));
    }

    @Test
    @DisplayName("and exp has passed but within the clock skew then the status is resolved")
    void expiredWithinClockSkew() {
        var endpoint = new MockStatusListEndpoint().serve(PT_URI, JWT, statusList("pt-pid-mdoc.jwt"));

        var validator = validator(endpoint, PT_EXPIRES_AT.plus(WITHIN_SKEW));

        assertTrue(validator.validate(URI.create(TestData.PT_URI), PT_IDX_VALID));
        assertEquals(0, validator.retrieveStatus(URI.create(TestData.PT_URI), PT_IDX_VALID));
    }

    @Test
    @DisplayName("and the JWT iat is in the future beyond the clock skew then an exception is expected")
    void jwtIssuedInFuture() {
        var endpoint = new MockStatusListEndpoint().serve(PT_URI, JWT, statusList("pt-pid-mdoc.jwt"));

        assertThrows(TokenStatusListException.class,
                () -> validator(endpoint, PT_ISSUED_AT.minus(BEYOND_SKEW)).validate(URI.create(TestData.PT_URI), PT_IDX_VALID));
    }

    @Test
    @DisplayName("and the CWT iat is in the future beyond the clock skew then an exception is expected")
    void cwtIssuedInFuture() {
        var endpoint = new MockStatusListEndpoint().serve(UA_URI, CWT, statusList("ua-pid-mdoc.cwt"));

        assertThrows(TokenStatusListException.class,
                () -> validator(endpoint, UA_ISSUED_AT.minus(BEYOND_SKEW)).validate(URI.create(TestData.UA_URI), UA_IDX_VALID));
    }

    @Test
    @DisplayName("and iat is in the future but within the clock skew then the status is resolved")
    void issuedInFutureWithinClockSkew() {
        var endpoint = new MockStatusListEndpoint().serve(UA_URI, CWT, statusList("ua-pid-mdoc.cwt"));

        var validator = validator(endpoint, UA_ISSUED_AT.minus(WITHIN_SKEW));

        assertTrue(validator.validate(URI.create(TestData.UA_URI), UA_IDX_VALID));
        assertEquals(0, validator.retrieveStatus(URI.create(TestData.UA_URI), UA_IDX_VALID));
    }

    @Test
    @DisplayName("and the JWT signature is tampered with then an exception is expected")
    void tamperedJwtSignature() {
        String jwt = new String(statusList("pt-pid-mdoc.jwt"), StandardCharsets.UTF_8).strip();
        char last = jwt.charAt(jwt.length() - 5);
        String tampered = jwt.substring(0, jwt.length() - 5) + (last == 'A' ? 'B' : 'A') + jwt.substring(jwt.length() - 4);
        var endpoint = new MockStatusListEndpoint().serve(PT_URI, JWT, tampered.getBytes(StandardCharsets.UTF_8));

        assertThrows(TokenStatusListException.class,
                () -> validator(endpoint, PT_ISSUED_AT.plus(AFTER_ISSUANCE)).validate(URI.create(TestData.PT_URI), PT_IDX_VALID));
    }

    @Test
    @DisplayName("and idx is beyond the list then an exception is expected")
    void indexOutOfRange() {
        var endpoint = new MockStatusListEndpoint().serve(PT_URI, JWT, statusList("pt-pid-mdoc.jwt"));

        assertThrows(TokenStatusListException.class,
                () -> validator(endpoint, PT_ISSUED_AT.plus(AFTER_ISSUANCE)).validate(URI.create(TestData.PT_URI), 100_000_000L));
    }

    @Test
    @DisplayName("and the endpoint answers 404 then an exception is expected")
    void notFound() {
        var endpoint = new MockStatusListEndpoint();

        assertThrows(TokenStatusListException.class,
                () -> validator(endpoint, PT_ISSUED_AT.plus(AFTER_ISSUANCE)).validate(URI.create(TestData.PT_URI), PT_IDX_VALID));
    }

    @Test
    @DisplayName("and uri or idx is missing then an IllegalArgumentException is expected")
    void missingInput() {
        var validator = validator(new MockStatusListEndpoint(), PT_ISSUED_AT);

        assertThrows(IllegalArgumentException.class, () -> validator.validate(null, 1L));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(URI.create(TestData.PT_URI), -1L));
    }

    @Test
    @DisplayName("and caching is enabled and the response has Cache-Control max-age then the list is fetched once")
    void cachedListIsFetchedOnce() {
        var endpoint = new MockStatusListEndpoint()
                .serve(PT_URI, JWT, statusList("pt-pid-mdoc.jwt"), "max-age=30");
        var validator = validator(endpoint, PT_ISSUED_AT.plus(AFTER_ISSUANCE));

        validator.validate(URI.create(TestData.PT_URI), PT_IDX_VALID);
        validator.validate(URI.create(TestData.PT_URI), PT_IDX_VALID);

        assertEquals(1, endpoint.requestCount());
    }

    @Test
    @DisplayName("and caching is enabled but the response has no Cache-Control then the list is fetched every time")
    void listWithoutCacheControlIsNotCached() {
        var endpoint = new MockStatusListEndpoint().serve(PT_URI, JWT, statusList("pt-pid-mdoc.jwt"));
        var validator = validator(endpoint, PT_ISSUED_AT.plus(AFTER_ISSUANCE));

        validator.validate(URI.create(TestData.PT_URI), PT_IDX_VALID);
        validator.validate(URI.create(TestData.PT_URI), PT_IDX_VALID);

        assertEquals(2, endpoint.requestCount());
    }

    @Test
    @DisplayName("and caching is disabled then the list is fetched every time")
    void uncachedListIsFetchedEveryTime() {
        var endpoint = new MockStatusListEndpoint()
                .serve(PT_URI, JWT, statusList("pt-pid-mdoc.jwt"), "max-age=30");
        var config = new TokenStatusListValidatorConfig(Duration.ofSeconds(3), Duration.ofSeconds(5), false, Duration.ofSeconds(60));
        var validator = new TokenStatusListValidator(config, fixedClock(PT_ISSUED_AT.plus(AFTER_ISSUANCE)), endpoint.engine());

        validator.validate(URI.create(TestData.PT_URI), PT_IDX_VALID);
        validator.validate(URI.create(TestData.PT_URI), PT_IDX_VALID);

        assertEquals(2, endpoint.requestCount());
    }

    private static TokenStatusListValidator validator(MockStatusListEndpoint endpoint, Instant now) {
        return new TokenStatusListValidator(TokenStatusListValidatorConfig.defaults(), fixedClock(now), endpoint.engine());
    }

    private static Clock fixedClock(Instant now) {
        return Clock.fixed(now, ZoneOffset.UTC);
    }
}
