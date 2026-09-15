package no.idporten.eudiw.verifier.openid4vp;

import id.walt.mdoc.cose.COSESign1;
import id.walt.mdoc.dataelement.*;
import id.walt.mdoc.doc.MDoc;
import id.walt.mdoc.issuersigned.IssuerSigned;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import no.idporten.eudiw.verifier.statuslist.StatuslistEntry;
import no.idporten.eudiw.verifier.testdata.Certificates;
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

import static no.idporten.eudiw.verifier.openid4vp.ConvertVPTokenToMDocTest.SuccessfulConversions.vpToken2_solvenia;
import static no.idporten.eudiw.verifier.openid4vp.ConvertVPTokenToMDocTest.SuccessfulConversions.vpToken3_solvenia;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("When handling mdoc credentials")
class MDocServiceTest {

    private static final String VP_TOKEN_WITHOUT_DOCUMENTS =
            "o2d2ZXJzaW9uYzEuMGlkb2N1bWVudHOAZnN0YXR1cwA";

    private final MDocService service = new MDocService();

    @Mock MDoc mDoc;
    @Mock IssuerSigned issuerSigned;
    @Mock COSESign1 issuerAuth;

    @Test
    void parsesRealVpTokenAndExtractsClaims() {
        MDoc parsed = service.mDocFromVpToken(vpToken2_solvenia);

        Map<String, Object> claims = service.claimsFromMDoc(parsed);

        Map<?, ?> pid = (Map<?, ?>) claims.get("eu.europa.ec.eudi.pid.1");
        assertAll(
                () -> assertNotNull(parsed),
                () -> assertEquals("BLUNDER", pid.get("family_name")),
                () -> assertEquals("DIVERSE", pid.get("given_name")),
                () -> assertEquals("1985-12-06", pid.get("birth_date")));
    }

    @Test
    void rejectsVpTokenWithoutDocuments() {
        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> service.mDocFromVpToken(VP_TOKEN_WITHOUT_DOCUMENTS));

        assertEquals("No mdoc documents in vp_token", exception.getErrorDescription());
    }

    @Test
    void rejectsMalformedVpToken() {
        VerificationException exception = assertThrows(
                VerificationException.class,
                () -> service.mDocFromVpToken("not-an-mdoc"));

        assertEquals("Failed to parse mdoc vp_token", exception.getErrorDescription());
    }

    @Test
    void reportsValidAndInvalidVerification() {
        when(mDoc.getMSO()).thenReturn(null);
        when(mDoc.verifyDocType()).thenReturn(true);
        when(mDoc.verifyIssuerSignedItems()).thenReturn(true);
        when(mDoc.verifyValidity()).thenReturn(true, false);

        assertEquals(ValidationStatus.VALID, service.verifyMDoc(mDoc));
        assertEquals(ValidationStatus.INVALID, service.verifyMDoc(mDoc));
    }

    @Test
    void extractsCertificateFromRealMdoc() throws Exception {
        X509Certificate certificate =
                service.extractCertificateFromMdoc(service.mDocFromVpToken(vpToken2_solvenia));

        assertEquals(
                new Certificates().getBevisportenCertificate(),
                Base64.getEncoder().encodeToString(certificate.getEncoded()));
    }

    @Test
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
    void extractsAndOmitsStatusListFromRealMdocs() {
        StatuslistEntry status =
                service.extractStatuslistUriAndIdx(service.mDocFromVpToken(vpToken3_solvenia));
        StatuslistEntry absent =
                service.extractStatuslistUriAndIdx(service.mDocFromVpToken(vpToken2_solvenia));

        assertAll(
                () -> assertEquals("561305", status.idx()),
                () -> assertEquals(
                        "https://qa.id.cloud.dvv.fi/status-list-token/4eea9ea5-e479-4200-a5ea-63b8e9cb1a83",
                        status.uri().toString()),
                () -> assertNull(absent));
    }

    @Test
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
