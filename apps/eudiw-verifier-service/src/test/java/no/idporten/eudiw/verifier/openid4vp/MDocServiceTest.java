package no.idporten.eudiw.verifier.openid4vp;

import id.walt.mdoc.verification.VerificationResult;
import no.idporten.eudiw.verifier.VerificationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;


import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("When handling mdoc credentials")
class MDocServiceTest {

    private static final String VALID_VP_TOKEN =
            "o2d2ZXJzaW9uYzEuMGlkb2N1bWVudHOBo2dkb2NUeXBld2V1LmV1cm9wYS5lYy5ldWRpLnBpZC4xbGlzc3VlclNpZ25lZKJqbmFtZVNwYWNlc6F3ZXUuZXVyb3BhLmVjLmV1ZGkucGlkLjGF2BhYW6RoZGlnZXN0SUQAZnJhbmRvbVAE0IGVTsv2RMsnnslXAAV3cWVsZW1lbnRJZGVudGlmaWVyamJpcnRoX2RhdGVsZWxlbWVudFZhbHVl2QPsajE5MjItMDYtMjLYGFhUpGhkaWdlc3RJRAJmcmFuZG9tUKEo5oj7OwDeJ4Ut6pXLbY1xZWxlbWVudElkZW50aWZpZXJrZmFtaWx5X25hbWVsZWxlbWVudFZhbHVlZUhJTk5F2BhYV6RoZGlnZXN0SUQDZnJhbmRvbVAHlUi9x0OVb7va76nVvJ5HcWVsZW1lbnRJZGVudGlmaWVyamdpdmVuX25hbWVsZWxlbWVudFZhbHVlaUlOVEVHUkVSVNgYWFKkaGRpZ2VzdElEBmZyYW5kb21Qc9N7RuOKmXzI05UynHI9anFlbGVtZW50SWRlbnRpZmllcmtuYXRpb25hbGl0eWxlbGVtZW50VmFsdWWBYk5P2BhYXaRoZGlnZXN0SUQIZnJhbmRvbVDnLZo7cgovg5c08qgNbzcxcWVsZW1lbnRJZGVudGlmaWVybnBsYWNlX29mX2JpcnRobGVsZW1lbnRWYWx1ZaFnY291bnRyeWJOT2ppc3N1ZXJBdXRohEOhASahGCFZAxcwggMTMIICuaADAgECAgkA4Ug40cCGZqwwCgYIKoZIzj0EAwIwgYQxHjAcBgNVBGETFU5UUk5PLU5PRk9SLjk5MTgyNTgyNzELMAkGA1UEBhMCTk8xJDAiBgNVBAoTG0RJR0lUQUxJU0VSSU5HU0RJUkVLVE9SQVRFVDEvMC0GA1UEAxMmZWlkYXMyc2FuZGthc3NlIFBJRCBQcm92aWRlciBDQSAyIHRlc3QwHhcNMjYwNDI4MTEwNDAwWhcNMjcwNDI4MTEwNDAwWjBqMQswCQYDVQQGEwJOTzEkMCIGA1UECgwbRElHSVRBTElTRVJJTkdTRElSRUtUT1JBVEVUMRUwEwYDVQQDDAxQSUQtdXRzdGVkZXIxHjAcBgNVBGEMFU5UUk5PLU5PRk9SLjk5MTgyNTgyNzBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABJJcleGs0Y42dKFA2UMJGfO6yvSCCkQgACCzem5L9OL0lLaZzqTovBv0Sd85DdDc5XkZVI4OtdcYWmpO2pi-R9ijggErMIIBJzAfBgNVHSMEGDAWgBS78B8CWfJsAVeFHJ1h_qMAOb5qRDAdBgNVHQ4EFgQUHURyzPMIgx1cG64kN1iDC7MBgecwDAYDVR0TAQH_BAIwADBdBgNVHR8EVjBUMFKgUKBOhkxodHRwczovL2NhLnRlc3QuZWlkYXMyc2FuZGthc3NlLm5ldC92MS9jZXJ0cy9pbnRlcm1lZGlhdGVzL3BpZF9wcm92aWRlcjIuY3JsMGgGCCsGAQUFBwEBBFwwWjBYBggrBgEFBQcwAoZMaHR0cHM6Ly9jYS50ZXN0LmVpZGFzMnNhbmRrYXNzZS5uZXQvdjEvY2VydHMvaW50ZXJtZWRpYXRlcy9waWRfcHJvdmlkZXIyLmNlcjAOBgNVHQ8BAf8EBAMCBaAwCgYIKoZIzj0EAwIDSAAwRQIgZhzCyIv9eCZi6cxvIBAuJHAfKrFkOIatvlG_QoSxeSUCIQCyPNpmWqpemS2iPvZCL1S42UpCkfRywy6N_hPG-fB5hlkC9NgYWQLvp2d2ZXJzaW9uYzEuMG9kaWdlc3RBbGdvcml0aG1nU0hBLTI1Nmx2YWx1ZURpZ2VzdHOhd2V1LmV1cm9wYS5lYy5ldWRpLnBpZC4xqQBYIFP7v_aUKZ6SlzUgGA-v0SLe_LyGE5aFHg8CmwwBeu-yAVggXmy_BvJTiDQcpD2dpDHKDdTKhqCeTdLEg7WSNcFt9PkCWCDUJZEqUZQzYD0sRDJOtIZKmzZfbvDUk3BMbWnOLgw3UwNYIEWRVeBFkLoBmSGGosUEmwXV73BcsdZdBJCicdstLWOCBFggQxs7u7b8QhHEO-z7Tk-aF3YkMRZ2B6N4zhAJziyanpoFWCCMeII1ryZryBnTbGThLlGlI0kUylGLC8uXEnKsbM3UKwZYIMN1Sinty5HoFd-lEqkupHAkKiTWUhPxjoTeyDBO6ZbkB1gg5uOCFwE18heAVoiCo_h_Dx_s1EW7JJQ9C32qV0e0dTAIWCBzOUBQ_LDsQPc0x67kIrG7TqKRbEQo1yCvNS6FUOaac21kZXZpY2VLZXlJbmZvoWlkZXZpY2VLZXmkAQIgASFYINbwJRw7jgjXBdxA5mBKPFkfz1AGFfnB2kjwjQnMP6QyIlggEFeLCA-2rxmj3jO5f_0VCkqW907wQUae6Yh6_sAYyxJnZG9jVHlwZXdldS5ldXJvcGEuZWMuZXVkaS5waWQuMWx2YWxpZGl0eUluZm-kZnNpZ25lZMB0MjAyNi0wOC0xOVQwODowMTozNVppdmFsaWRGcm9twHQyMDI2LTA4LTE5VDA4OjAxOjM1Wmp2YWxpZFVudGlswHQyMDI3LTA4LTE5VDA4OjAyOjM1Wm5leHBlY3RlZFVwZGF0ZcB0MjAyNy0wOC0xOVQwODowMjozNVpmc3RhdHVzoWtzdGF0dXNfbGlzdKJjaWR4GgAHxBVjdXJpeC9odHRwczovL3N0YXR1cy50ZXN0LmVpZGFzMnNhbmRrYXNzZS5uZXQvbGlzdHMvM1hAeZh5KTosVbJC64FIJG7Uivq2wa6gLzg8ErKedc4RlcJg8nr7j2DplSdQU5SoUWtaCoy8l4I_p0eYLHIDT1X9FWxkZXZpY2VTaWduZWSiam5hbWVTcGFjZXPYGEGgamRldmljZUF1dGihb2RldmljZVNpZ25hdHVyZYRDoQEmoPZYQB1Gsi8PYQNDqTW8zAh5sU91RSHfqNTT8Q_zNxg00Jv_ZjviBZjzJbWWNfWvSDkD-36xaX9A8Qeu1HFNcgtBCilmc3RhdHVzAA";
    private static final String VP_TOKEN_WITHOUT_DOCUMENTS =
            "o2d2ZXJzaW9uYzEuMGlkb2N1bWVudHOAZnN0YXR1cwA";

    private static final String PID_VALID_VP_TOKEN = "o2d2ZXJzaW9uYzEuMGlkb2N1bWVudHOBo2dkb2NUeXBld2V1LmV1cm9wYS5lYy5ldWRpLnBpZC4xbGlzc3VlclNpZ25lZKJqbmFtZVNwYWNlc6F3ZXUuZXVyb3BhLmVjLmV1ZGkucGlkLjGF2BhYVaRoZGlnZXN0SUQCZnJhbmRvbVBmowOjhh8mlKogVyun2aqCcWVsZW1lbnRJZGVudGlmaWVya2ZhbWlseV9uYW1lbGVsZW1lbnRWYWx1ZWZNw5hMTEXYGFhVpGhkaWdlc3RJRANmcmFuZG9tUFwnXFt2C7fqiKEUWA_8FhZxZWxlbWVudElkZW50aWZpZXJqZ2l2ZW5fbmFtZWxlbGVtZW50VmFsdWVnT1BQUkVUVNgYWFukaGRpZ2VzdElEAGZyYW5kb21QyRhmgLgHHI2WMfJKMa2x83FlbGVtZW50SWRlbnRpZmllcmpiaXJ0aF9kYXRlbGVsZW1lbnRWYWx1ZdkD7GoxOTEzLTEwLTEz2BhYXaRoZGlnZXN0SUQIZnJhbmRvbVDq_t5UmqkUdTiuGRlLvXZAcWVsZW1lbnRJZGVudGlmaWVybnBsYWNlX29mX2JpcnRobGVsZW1lbnRWYWx1ZaFnY291bnRyeWJOT9gYWFKkaGRpZ2VzdElEBmZyYW5kb21QHuXtsj-Pdhaz6A_fu2tiJ3FlbGVtZW50SWRlbnRpZmllcmtuYXRpb25hbGl0eWxlbGVtZW50VmFsdWWBYk5Pamlzc3VlckF1dGiEQ6EBJqEYIVkDDjCCAwowggKxoAMCAQICCAT4DA6L9DIRMAoGCCqGSM49BAMCMIGHMR4wHAYDVQRhExVOVFJOTy1OT0ZPUi45OTE4MjU4MjcxCzAJBgNVBAYTAk5PMSQwIgYDVQQKExtESUdJVEFMSVNFUklOR1NESVJFS1RPUkFURVQxMjAwBgNVBAMTKWVpZGFzMnNhbmRrYXNzZSBQSUQgUHJvdmlkZXIgQ0EgMiBzeXN0ZXN0MB4XDTI2MDQyODEwNDExMFoXDTI3MDQyODEwNDExMFowajELMAkGA1UEBhMCTk8xJDAiBgNVBAoMG0RJR0lUQUxJU0VSSU5HU0RJUkVLVE9SQVRFVDEVMBMGA1UEAwwMUElELXV0c3RlZGVyMR4wHAYDVQRhDBVOVFJOTy1OT0ZPUi45OTE4MjU4MjcwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAAQFV5otfpjnWFh6-2GAyKf0yzNnPoT4Qme2Ku4bgLjQNW1XuZSGC5E8Huv0ss_meX-ihLU_e1qgT9QRPWZCV3yko4IBITCCAR0wHwYDVR0jBBgwFoAUuvAX8Rs0NtXvqzslYYuoSrYL6kswHQYDVR0OBBYEFJbHk6LMQeQ3Zt3D11qy4QST9XhxMAwGA1UdEwEB_wQCMAAwWAYDVR0fBFEwTzBNoEugSYZHaHR0cHM6Ly9jYS5laWRhczJzYW5ka2Fzc2UuZGV2L3YxL2NlcnRzL2ludGVybWVkaWF0ZXMvcGlkX3Byb3ZpZGVyMi5jcmwwYwYIKwYBBQUHAQEEVzBVMFMGCCsGAQUFBzAChkdodHRwczovL2NhLmVpZGFzMnNhbmRrYXNzZS5kZXYvdjEvY2VydHMvaW50ZXJtZWRpYXRlcy9waWRfcHJvdmlkZXIyLmNlcjAOBgNVHQ8BAf8EBAMCBaAwCgYIKoZIzj0EAwIDRwAwRAIgQzlb4C9Y2D8LtLBjI-od3PiXc_-TjOB6h1oC40c5ReUCIBMWrudcxFh2DjaKgqZYBIaZ-6xxvD4p-FNeIKzbUimUWQLv2BhZAuqnZ3ZlcnNpb25jMS4wb2RpZ2VzdEFsZ29yaXRobWdTSEEtMjU2bHZhbHVlRGlnZXN0c6F3ZXUuZXVyb3BhLmVjLmV1ZGkucGlkLjGpAFggmHqfSdsDdoPyWQRgm09cVR-ZYXXxWcD52sPVY0kYm3EBWCDhzM1zzvr4OBiHiGPKgWrKNaemd6WXBFz5ZmsWpd3guAJYIGXm_UVYrNuK2tHvBa7eUpMzfYkXwgWcdWPWk8cuughnA1ggW6P3giUQZPgsMM8iV0IHTOnWwvMOSp3PcAHJK0E329QEWCCmP6jqmlubsohP8e9hi1wGLLij-ebH8xZiHdjDrmlcWAVYIHLo76w2Nrm7punCdnMM8JueC-osxVKlvpIXoBwcBKDEBlggoS7wl8CE3UGS3XtuJrQ0FwvlgPCEBvHD1tt8n2rYYt0HWCCdePXT7zdPoszp-_qo9VAyBeP5MnSgu0kLUHoCY0S_SQhYIGLs7e25F6O5hg0_LCSQY-uzHl10RIp4UHD0b3plvKAdbWRldmljZUtleUluZm-haWRldmljZUtleaQBAiABIVgg-8woTIN6j0bkksVR-uxgDnq-yjQ35X-7e03KhPcxlcEiWCBUgvfgGs31y-fEmoMlcXRQdTLJiIWkt8q85Bl1lelVrWdkb2NUeXBld2V1LmV1cm9wYS5lYy5ldWRpLnBpZC4xbHZhbGlkaXR5SW5mb6Rmc2lnbmVkwHQyMDI2LTEwLTA5VDEyOjU3OjQzWml2YWxpZEZyb23AdDIwMjYtMTAtMDlUMTI6NTc6NDNaanZhbGlkVW50aWzAdDIwMjctMTAtMDlUMTI6NTg6NDNabmV4cGVjdGVkVXBkYXRlwHQyMDI3LTEwLTA5VDEyOjU4OjQzWmZzdGF0dXOha3N0YXR1c19saXN0omNpZHgaAAX8qWN1cml4Kmh0dHBzOi8vc3RhdHVzLmVpZGFzMnNhbmRrYXNzZS5kZXYvbGlzdHMvMVhAEUWY2qigWulBiDFjP-TBIO-ZNqLgSwiYeTKg2vn0LHBS0aLuJOdcgEpR0BNOXCM9xpaREp1RD9g-HtvMuj9bsGxkZXZpY2VTaWduZWSiam5hbWVTcGFjZXPYGEGgamRldmljZUF1dGihb2RldmljZVNpZ25hdHVyZYRDoQEmoPZYQC2OPlwB-TwGoFs01LEVNTY4gSgmC5ffA9RLiX9-fjYO_mxjF5WZ77ynifwjGJTRg4H-wp2u9uIAsWaig9sE4Ddmc3RhdHVzAA";


    private final MDocService service = new MDocService();


@Test
@DisplayName("with valid mdoc and valid OID4VP context, then verification is valid")
void acceptsValidMdoc() {
    String validMdocToken = VALID_VP_TOKEN;

    VerificationTransaction tx = new VerificationTransaction();
    tx.setNonce("test-nonce");
    tx.setAudience("https://verifier.example.com");
    tx.setRedirectUri(URI.create("https://verifier.example.com/callback"));

    VerificationResult result = service.verifyMDoc(validMdocToken, tx);

    assertNotNull(result);
    assertTrue(result.getValid());
}

    @Test
    @DisplayName("with malformed mdoc token, then invalid request is expected")
    void rejectsMalformedMdocToken() {
        VerificationTransaction tx = new VerificationTransaction();
        tx.setNonce("test-nonce");
        tx.setAudience("https://verifier.example.com");
        tx.setRedirectUri(URI.create("https://verifier.example.com/callback"));

        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> service.verifyMDoc("not-a-valid-mdoc", tx)
        );

        assertEquals("invalid_request", exception.getError());
    }

    @Test
    @DisplayName("with invalid verifier result, then status is not valid")
    void rejectsInvalidVerificationResult() {
        String validMdocToken = PID_VALID_VP_TOKEN;


        VerificationTransaction tx = new VerificationTransaction();
        tx.setNonce("wrong-nonce");
        tx.setAudience("https://verifier.example.com");
        tx.setRedirectUri(URI.create("https://verifier.example.com/callback"));

        VerificationResult result = service.verifyMDoc(validMdocToken, tx);

        assertNotNull(result);
        assertFalse(result.getValid());
    }

    @Test
    @DisplayName("with missing redirect uri, then verification request is rejected")
    void rejectsMissingRedirectUri() {
        String validMdocToken = VALID_VP_TOKEN;

        VerificationTransaction tx = new VerificationTransaction();
        tx.setNonce("test-nonce");
        tx.setAudience("https://verifier.example.com");

        assertThrows(
                VerificationException.class,
                () -> service.verifyMDoc(validMdocToken, tx)
        );
    }
}
