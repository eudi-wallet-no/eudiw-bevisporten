package net.eidas2sandkasse.tsl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.time.Instant;

/**
 * Real status lists, fetched 2026-11-18. uri and idx from test credentials. The signing certificates expire from late 2027.
 */
final class TestData {

    static final String JWT = "application/statuslist+jwt";
    static final String CWT = "application/statuslist+cwt";

    static final String OTHER_URI = "https://status.junit.eidas2sandkasse.dev/lists/1";
    static final String HTTP_URI = "http://status.junit.eidas2sandkasse.dev/lists/1";

    // UA, mdoc PID. CWT only, it has x5chain in the unprotected header.
    static final String UA_URI =
            "https://api2s.diia.gov.ua/api/v1/euid/wallet/token_status_list/UA/eu.europa.ec.eudi.pid.1/95d1989e-9318-46c3-9445-65e34a6687dc";
    static final long UA_IDX_VALID = 65826;
    static final long UA_IDX_REVOKED = 36148;
    static final Instant UA_ISSUED_AT = Instant.ofEpochSecond(1795005049);
    // CN=Diia Status Signer (test)
    static final BigInteger UA_SIGNER_SERIAL = new BigInteger("01B7346F017FF087AC5D2FCD8E91276A", 16);

    // PT, mdoc PID. JWT, and CWT with x5chain in the protected header.
    static final String PT_URI =
            "https://walletreg.autoservicework.com/token_status_list/PT/eu.europa.ec.eudi.pid.1/76d0ec68-da3c-4a0d-8e5b-41331b269a50";
    static final long PT_IDX_VALID = 986;
    static final long PT_IDX_REVOKED = 11; // From the list itself, not from a PID
    static final Instant PT_ISSUED_AT = Instant.ofEpochSecond(1794960002);
    static final Instant PT_EXPIRES_AT = Instant.ofEpochSecond(1795046402);
    static final BigInteger PT_SIGNER_SERIAL = new BigInteger("22DD3B7A807CEAC7AEEF8CB4FC0C6B17E6372ED6", 16);

    static byte[] statusList(String name) {
        try (InputStream in = TestData.class.getResourceAsStream("/statuslist/" + name)) {
            if (in == null) {
                throw new IllegalArgumentException("Missing test resource " + name);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private TestData() {
    }
}
