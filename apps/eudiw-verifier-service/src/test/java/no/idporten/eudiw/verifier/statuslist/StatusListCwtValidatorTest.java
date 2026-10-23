package no.idporten.eudiw.verifier.statuslist;

import id.walt.mdoc.cose.COSESign1;
import id.walt.mdoc.dataelement.ByteStringElement;
import id.walt.mdoc.dataelement.MapElement;
import id.walt.mdoc.dataelement.MapKey;
import id.walt.mdoc.dataelement.NumberElement;
import id.walt.mdoc.dataelement.StringElement;
import no.idporten.eudiw.verifier.VerificationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

import static no.idporten.eudiw.verifier.statuslist.StatusListTestUtil.createSignedStatusListCwtWithRsa;
import static no.idporten.eudiw.verifier.statuslist.StatusListCwtValidator.CWT_ISSUED_AT_CLAIM;
import static no.idporten.eudiw.verifier.statuslist.StatusListCwtValidator.CWT_STATUS_LIST_CLAIM;
import static no.idporten.eudiw.verifier.statuslist.StatusListCwtValidator.CWT_SUBJECT_CLAIM;
import static no.idporten.eudiw.verifier.statuslist.StatusListCwtValidator.STATUS_LIST_BITS;
import static no.idporten.eudiw.verifier.statuslist.StatusListCwtValidator.STATUS_LIST_DATA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@DisplayName("When validating and resolving status from status list CWT")
class StatusListCwtValidatorTest {

    private static final String RFC_CWT_STATUS_LIST_HEX = """
            d2845820a2012610781a6170706c69636174696f6e2f7374617475736c6973742b63
            7774a1044231325850a502782168747470733a2f2f6578616d706c652e636f6d2f73
            74617475736c697374732f31061a648c5bea041a8898dfea19fffe19a8c019fffda2
            646269747301636c73744a78dadbb918000217015d584093fa4d01032b18c35e2fe1
            101b77fd6cc9440022caa4694450c4e4e9feab4e99d1fa6d9772ce2bf3a12e0323de
            d7c982c5e101a5e67f0cbc1e2b6f57ce99c279
            """;

    @Test
    @DisplayName("and the RFC CWT example is parsed then status-list claims should be resolved")
    void parsesRfcCwtExampleAndResolvesStatus() {
        StatusListCwtValidator validator = new StatusListCwtValidator(Duration.ofSeconds(60));
        byte[] cwtBytes = HexFormat.of().parseHex(RFC_CWT_STATUS_LIST_HEX.replaceAll("\\s", ""));

        COSESign1 cwt = validator.parseCwt(cwtBytes);
        validator.validateProtectedHeader(cwt);
        MapElement claims = validator.decodePayload(cwt);
        StringElement subject = assertInstanceOf(StringElement.class,
                claims.getValue().get(new MapKey(CWT_SUBJECT_CLAIM)));
        NumberElement issuedAt = assertInstanceOf(NumberElement.class,
                claims.getValue().get(new MapKey(CWT_ISSUED_AT_CLAIM)));
        MapElement statusList = assertInstanceOf(MapElement.class,
                claims.getValue().get(new MapKey(CWT_STATUS_LIST_CLAIM)));
        NumberElement bits = assertInstanceOf(NumberElement.class,
                statusList.getValue().get(new MapKey(STATUS_LIST_BITS)));
        ByteStringElement lst = assertInstanceOf(ByteStringElement.class,
                statusList.getValue().get(new MapKey(STATUS_LIST_DATA)));

        assertEquals("https://example.com/statuslists/1", subject.getValue());
        assertEquals(1686920170L, issuedAt.getValue().longValue());
        assertEquals(1, bits.getValue().intValue());
        assertEquals(1, StatusListDecoder.resolveStatus(0, bits.getValue().intValue(), lst.getValue()));

    }

    @Test
    @DisplayName("and CWT is valid then status should be resolved")
    void validateAndResolveStatusReturnsStatusWhenCwtIsValid() throws Exception {
        StatusListCwtValidator validator = new StatusListCwtValidator(Duration.ofSeconds(60));
        URI expectedUri = URI.create("https://status.example/list/1");
        Instant now = Instant.now();
        byte[] cwt = createSignedStatusListCwtWithRsa(expectedUri, now, 1, new byte[]{0});

        int status = validator.validateAndResolveStatus(expectedUri, 1, cwt, now);

        assertEquals(0, status);
    }

    @Test
    @DisplayName("and status value is non-zero then non-zero status should be resolved")
    void validateAndResolveStatusReturnsNonZeroStatus() throws Exception {
        StatusListCwtValidator validator = new StatusListCwtValidator(Duration.ofSeconds(60));
        URI expectedUri = URI.create("https://status.example/list/1");
        Instant now = Instant.now();
        byte[] cwt = createSignedStatusListCwtWithRsa(expectedUri, now, 1, new byte[]{2});

        int status = validator.validateAndResolveStatus(expectedUri, 1, cwt, now);

        assertEquals(1, status);
    }

    @Test
    @DisplayName("and status list subject does not match expected URI then verification exception should be thrown")
    void validateAndResolveStatusThrowsWhenSubjectDoesNotMatchExpectedUri() throws Exception {
        StatusListCwtValidator validator = new StatusListCwtValidator(Duration.ofSeconds(60));
        Instant now = Instant.now();
        byte[] cwt = createSignedStatusListCwtWithRsa(
                URI.create("https://status.example/list/other"), now, 1, new byte[]{0});

        VerificationException exception = assertThrows(VerificationException.class,
                () -> validator.validateAndResolveStatus(
                        URI.create("https://status.example/list/expected"), 0, cwt, now));

        assertEquals("Status list CWT sub does not match expected status list uri", exception.getErrorDescription());
    }

    @Test
    @DisplayName("and CWT signature is invalid then verification exception should be thrown")
    void validateAndResolveStatusThrowsWhenSignatureIsInvalid() throws Exception {
        StatusListCwtValidator validator = new StatusListCwtValidator(Duration.ofSeconds(60));
        URI expectedUri = URI.create("https://status.example/list/1");
        byte[] cwt = createSignedStatusListCwtWithRsa(expectedUri, Instant.now(), 1, new byte[]{0});
        cwt[cwt.length - 1] ^= 1;

        VerificationException exception = assertThrows(VerificationException.class,
                () -> validator.validateAndResolveStatus(expectedUri, 0, cwt, Instant.now()));

        assertEquals("Status list CWT signature validation failed", exception.getErrorDescription());
    }
}
