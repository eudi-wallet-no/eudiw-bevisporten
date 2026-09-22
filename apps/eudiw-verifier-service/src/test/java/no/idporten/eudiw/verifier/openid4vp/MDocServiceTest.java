package no.idporten.eudiw.verifier.openid4vp;

import id.walt.mdoc.cose.COSESign1;
import id.walt.mdoc.dataelement.*;
import id.walt.mdoc.doc.MDoc;
import id.walt.mdoc.issuersigned.IssuerSigned;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import no.idporten.eudiw.verifier.statuslist.StatuslistEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("When handling mdoc credentials")
class MDocServiceTest {

    private static final String VALID_VP_TOKEN =
            "o2d2ZXJzaW9uYzEuMGlkb2N1bWVudHOBo2dkb2NUeXBld2V1LmV1cm9wYS5lYy5ldWRpLnBpZC4xbGlzc3VlclNpZ25lZKJqbmFtZVNwYWNlc6F3ZXUuZXVyb3BhLmVjLmV1ZGkucGlkLjGF2BhYW6RoZGlnZXN0SUQAZnJhbmRvbVAE0IGVTsv2RMsnnslXAAV3cWVsZW1lbnRJZGVudGlmaWVyamJpcnRoX2RhdGVsZWxlbWVudFZhbHVl2QPsajE5MjItMDYtMjLYGFhUpGhkaWdlc3RJRAJmcmFuZG9tUKEo5oj7OwDeJ4Ut6pXLbY1xZWxlbWVudElkZW50aWZpZXJrZmFtaWx5X25hbWVsZWxlbWVudFZhbHVlZUhJTk5F2BhYV6RoZGlnZXN0SUQDZnJhbmRvbVAHlUi9x0OVb7va76nVvJ5HcWVsZW1lbnRJZGVudGlmaWVyamdpdmVuX25hbWVsZWxlbWVudFZhbHVlaUlOVEVHUkVSVNgYWFKkaGRpZ2VzdElEBmZyYW5kb21Qc9N7RuOKmXzI05UynHI9anFlbGVtZW50SWRlbnRpZmllcmtuYXRpb25hbGl0eWxlbGVtZW50VmFsdWWBYk5P2BhYXaRoZGlnZXN0SUQIZnJhbmRvbVDnLZo7cgovg5c08qgNbzcxcWVsZW1lbnRJZGVudGlmaWVybnBsYWNlX29mX2JpcnRobGVsZW1lbnRWYWx1ZaFnY291bnRyeWJOT2ppc3N1ZXJBdXRohEOhASahGCFZAxcwggMTMIICuaADAgECAgkA4Ug40cCGZqwwCgYIKoZIzj0EAwIwgYQxHjAcBgNVBGETFU5UUk5PLU5PRk9SLjk5MTgyNTgyNzELMAkGA1UEBhMCTk8xJDAiBgNVBAoTG0RJR0lUQUxJU0VSSU5HU0RJUkVLVE9SQVRFVDEvMC0GA1UEAxMmZWlkYXMyc2FuZGthc3NlIFBJRCBQcm92aWRlciBDQSAyIHRlc3QwHhcNMjYwNDI4MTEwNDAwWhcNMjcwNDI4MTEwNDAwWjBqMQswCQYDVQQGEwJOTzEkMCIGA1UECgwbRElHSVRBTElTRVJJTkdTRElSRUtUT1JBVEVUMRUwEwYDVQQDDAxQSUQtdXRzdGVkZXIxHjAcBgNVBGEMFU5UUk5PLU5PRk9SLjk5MTgyNTgyNzBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABJJcleGs0Y42dKFA2UMJGfO6yvSCCkQgACCzem5L9OL0lLaZzqTovBv0Sd85DdDc5XkZVI4OtdcYWmpO2pi-R9ijggErMIIBJzAfBgNVHSMEGDAWgBS78B8CWfJsAVeFHJ1h_qMAOb5qRDAdBgNVHQ4EFgQUHURyzPMIgx1cG64kN1iDC7MBgecwDAYDVR0TAQH_BAIwADBdBgNVHR8EVjBUMFKgUKBOhkxodHRwczovL2NhLnRlc3QuZWlkYXMyc2FuZGthc3NlLm5ldC92MS9jZXJ0cy9pbnRlcm1lZGlhdGVzL3BpZF9wcm92aWRlcjIuY3JsMGgGCCsGAQUFBwEBBFwwWjBYBggrBgEFBQcwAoZMaHR0cHM6Ly9jYS50ZXN0LmVpZGFzMnNhbmRrYXNzZS5uZXQvdjEvY2VydHMvaW50ZXJtZWRpYXRlcy9waWRfcHJvdmlkZXIyLmNlcjAOBgNVHQ8BAf8EBAMCBaAwCgYIKoZIzj0EAwIDSAAwRQIgZhzCyIv9eCZi6cxvIBAuJHAfKrFkOIatvlG_QoSxeSUCIQCyPNpmWqpemS2iPvZCL1S42UpCkfRywy6N_hPG-fB5hlkC9NgYWQLvp2d2ZXJzaW9uYzEuMG9kaWdlc3RBbGdvcml0aG1nU0hBLTI1Nmx2YWx1ZURpZ2VzdHOhd2V1LmV1cm9wYS5lYy5ldWRpLnBpZC4xqQBYIFP7v_aUKZ6SlzUgGA-v0SLe_LyGE5aFHg8CmwwBeu-yAVggXmy_BvJTiDQcpD2dpDHKDdTKhqCeTdLEg7WSNcFt9PkCWCDUJZEqUZQzYD0sRDJOtIZKmzZfbvDUk3BMbWnOLgw3UwNYIEWRVeBFkLoBmSGGosUEmwXV73BcsdZdBJCicdstLWOCBFggQxs7u7b8QhHEO-z7Tk-aF3YkMRZ2B6N4zhAJziyanpoFWCCMeII1ryZryBnTbGThLlGlI0kUylGLC8uXEnKsbM3UKwZYIMN1Sinty5HoFd-lEqkupHAkKiTWUhPxjoTeyDBO6ZbkB1gg5uOCFwE18heAVoiCo_h_Dx_s1EW7JJQ9C32qV0e0dTAIWCBzOUBQ_LDsQPc0x67kIrG7TqKRbEQo1yCvNS6FUOaac21kZXZpY2VLZXlJbmZvoWlkZXZpY2VLZXmkAQIgASFYINbwJRw7jgjXBdxA5mBKPFkfz1AGFfnB2kjwjQnMP6QyIlggEFeLCA-2rxmj3jO5f_0VCkqW907wQUae6Yh6_sAYyxJnZG9jVHlwZXdldS5ldXJvcGEuZWMuZXVkaS5waWQuMWx2YWxpZGl0eUluZm-kZnNpZ25lZMB0MjAyNi0wOC0xOVQwODowMTozNVppdmFsaWRGcm9twHQyMDI2LTA4LTE5VDA4OjAxOjM1Wmp2YWxpZFVudGlswHQyMDI3LTA4LTE5VDA4OjAyOjM1Wm5leHBlY3RlZFVwZGF0ZcB0MjAyNy0wOC0xOVQwODowMjozNVpmc3RhdHVzoWtzdGF0dXNfbGlzdKJjaWR4GgAHxBVjdXJpeC9odHRwczovL3N0YXR1cy50ZXN0LmVpZGFzMnNhbmRrYXNzZS5uZXQvbGlzdHMvM1hAeZh5KTosVbJC64FIJG7Uivq2wa6gLzg8ErKedc4RlcJg8nr7j2DplSdQU5SoUWtaCoy8l4I_p0eYLHIDT1X9FWxkZXZpY2VTaWduZWSiam5hbWVTcGFjZXPYGEGgamRldmljZUF1dGihb2RldmljZVNpZ25hdHVyZYRDoQEmoPZYQB1Gsi8PYQNDqTW8zAh5sU91RSHfqNTT8Q_zNxg00Jv_ZjviBZjzJbWWNfWvSDkD-36xaX9A8Qeu1HFNcgtBCilmc3RhdHVzAA";
    private static final String VP_TOKEN_WITHOUT_DOCUMENTS =
            "o2d2ZXJzaW9uYzEuMGlkb2N1bWVudHOAZnN0YXR1cwA";

    private final MDocService service = new MDocService();

    @Mock MDoc mDoc;
    @Mock IssuerSigned issuerSigned;
    @Mock COSESign1 issuerAuth;

    @Test
    @DisplayName("with a valid VP token, then PID claims are expected")
    void parsesRealVpTokenAndExtractsClaims() {
        MDoc parsed = service.mDocFromVpToken(VALID_VP_TOKEN);

        Map<String, Object> claims = service.claimsFromMDoc(parsed);

        Map<?, ?> pid = (Map<?, ?>) claims.get("eu.europa.ec.eudi.pid.1");
        assertAll(
                () -> assertNotNull(parsed),
                () -> assertEquals("HINNE", pid.get("family_name")),
                () -> assertEquals("INTEGRERT", pid.get("given_name")),
                () -> assertEquals("1922-06-22", pid.get("birth_date")));
    }

    @Test
    @DisplayName("with a VP token without documents, then invalid request is expected")
    void rejectsVpTokenWithoutDocuments() {
        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> service.mDocFromVpToken(VP_TOKEN_WITHOUT_DOCUMENTS));

        assertEquals("No mdoc documents in vp_token", exception.getErrorDescription());
    }

    @Test
    @DisplayName("with a malformed VP token, then invalid request is expected")
    void rejectsMalformedVpToken() {
        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> service.mDocFromVpToken("not-an-mdoc"));

        assertEquals("Failed to parse mdoc vp_token", exception.getErrorDescription());
    }

    @Test
    @DisplayName("with valid and invalid mdocs, then matching validation statuses are expected")
    void reportsValidAndInvalidVerification() {
        when(mDoc.getMSO()).thenReturn(null);
        when(mDoc.verifyDocType()).thenReturn(true);
        when(mDoc.verifyIssuerSignedItems()).thenReturn(true);
        when(mDoc.verifyValidity()).thenReturn(true, false);

        assertEquals(ValidationStatus.VALID, service.verifyMDoc(mDoc));
        assertEquals(ValidationStatus.INVALID, service.verifyMDoc(mDoc));
    }

    @Test
    @DisplayName("with a valid mdoc, then the issuer certificate is expected")
    void extractsCertificateFromRealMdoc() throws Exception {
        X509Certificate certificate =
                service.extractCertificateFromMdoc(service.mDocFromVpToken(VALID_VP_TOKEN));

        assertEquals(
                "2.5.4.97=#0c154e54524e4f2d4e4f464f522e393931383235383237,"
                        + "CN=PID-utsteder,O=DIGITALISERINGSDIREKTORATET,C=NO",
                certificate.getSubjectX500Principal().getName());
    }

    @Test
    @DisplayName("without issuer authentication or x5chain, then invalid request is expected")
    void rejectsMissingIssuerAuthAndX5chain() {
        when(mDoc.getIssuerSigned()).thenReturn(issuerSigned);
        when(issuerSigned.getIssuerAuth()).thenReturn(null, issuerAuth);
        when(issuerAuth.getX5Chain()).thenReturn(List.of());

        VerificationException missingAuth =
                assertThrows(VerificationException.class, () -> service.extractCertificateFromMdoc(mDoc));
        VerificationException missingChain =
                assertThrows(VerificationException.class, () -> service.extractCertificateFromMdoc(mDoc));

        assertAll(
                () -> assertEquals("issuerAuth is missing in mdoc", missingAuth.getErrorDescription()),
                () -> assertEquals("x5chain is missing in issuerAuth of mdoc", missingChain.getErrorDescription()));
    }

    @Test
    @DisplayName("with a malformed certificate, then invalid request is expected")
    void rejectsMalformedCertificate() {
        when(mDoc.getIssuerSigned()).thenReturn(issuerSigned);
        when(issuerSigned.getIssuerAuth()).thenReturn(issuerAuth);
        when(issuerAuth.getX5Chain()).thenReturn(List.of("not-a-cert".getBytes(StandardCharsets.UTF_8)));

        VerificationException exception =
                assertThrows(VerificationException.class, () -> service.extractCertificateFromMdoc(mDoc));

        assertEquals("unable to extract certificate from issuerAuth x5chain mdoc",
                exception.getErrorDescription());
    }

    @Test
    @DisplayName("with and without a status list, then matching results are expected")
    void extractsAndOmitsStatusListFromRealMdocs() {
        StatuslistEntry status =
                service.extractStatuslistUriAndIdx(service.mDocFromVpToken(VALID_VP_TOKEN));
        when(mDoc.getMSO()).thenReturn(null);
        StatuslistEntry absent = service.extractStatuslistUriAndIdx(mDoc);

        assertAll(
                () -> assertEquals("508949", status.idx()),
                () -> assertEquals(
                        "https://status.test.eidas2sandkasse.net/lists/3",
                        status.uri().toString()),
                () -> assertNull(absent));
    }

    @Test
    @DisplayName("with supported data element values, then matching Java values are expected")
    void convertsSupportedDataElementValues() {
        MapElement map = new MapElement(Map.of(
                new MapKey("flag"), new BooleanElement(true),
                new MapKey("name"), new StringElement("Ada")));

        assertAll(
                () -> assertEquals(true, service.extractValue(new BooleanElement(true))),
                () -> assertEquals(42, service.extractValue(new NumberElement(42))),
                () -> assertEquals("Ada", service.extractValue(new StringElement("Ada"))),
                () -> assertEquals(List.of("Ada", 42), service.extractValue(
                        new ListElement(List.of(new StringElement("Ada"), new NumberElement(42))))),
                () -> assertEquals(Map.of("flag", true, "name", "Ada"), service.extractValue(map)),
                () -> assertEquals(Base64.getEncoder().encodeToString("bytes".getBytes(StandardCharsets.UTF_8)),
                        service.extractValue(new ByteStringElement("bytes".getBytes(StandardCharsets.UTF_8)))),
                () -> assertNull(service.extractValue(null)));
    }

    @Test
    @DisplayName("with every validation status, then matching detail text is expected")
    void returnsValidationDetailTextForEveryStatus() {
        assertAll(
                () -> assertEquals("mdoc: mdoc er gyldig", service.getValidationDetail(ValidationStatus.VALID)),
                () -> assertEquals("mdoc: mdoc er ugyldig", service.getValidationDetail(ValidationStatus.INVALID)),
                () -> assertEquals("mdoc: validering feila",
                        service.getValidationDetail(ValidationStatus.INCONCLUSIVE)),
                () -> assertEquals("mdoc: ukjent status",
                        service.getValidationDetail(ValidationStatus.NOT_APPLICABLE)));
    }
}
