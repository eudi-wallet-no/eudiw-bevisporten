package no.idporten.eudiw.issuer.credentials.formats;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import id.walt.cose.Cose;
import id.walt.cose.CoseKey;
import id.walt.mdoc.objects.document.IssuerSigned;
import id.walt.mdoc.objects.elements.IssuerSignedItem;
import id.walt.mdoc.objects.mso.MobileSecurityObject;
import id.walt.mdoc.objects.mso.Status;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.credentials.configurations.CredentialIssuerContext;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.credentials.status.CredentialStatus;
import no.idporten.eudiw.issuer.credentials.types.*;
import no.idporten.lib.keystore.KeystoreManager;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static no.idporten.eudiw.issuer.credentials.formats.MDocTestSupport.items;
import static no.idporten.eudiw.issuer.credentials.formats.MDocTestSupport.value;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When creting credentials in mdoc format")
@ActiveProfiles("junit")
@SpringBootTest
public class MDocServiceTest {

    @Autowired
    private MDocService mDocService;

    @Autowired
    private KeystoreManager keystoreManager;

    @BeforeAll
    static void addBouncyCastle() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    @MockitoBean
    private AuditLogger auditLogger;

    private ExtendedCredentialConfiguration credentialConfiguration(String docType, String credentialSigningKeystore) {
        ExtendedCredentialConfiguration credentialConfiguration = new ExtendedCredentialConfiguration();
        credentialConfiguration.setCredentialType(docType);
        CredentialIssuerContext credentialIssuerContext = new CredentialIssuerContext();
        credentialIssuerContext.setCredentialSigningKeystore(credentialSigningKeystore);
        credentialConfiguration.setCredentialIssuerContext(credentialIssuerContext);
        return credentialConfiguration;
    }

    private static MobileSecurityObject mso(IssuerSigned issuerSigned) {
        return issuerSigned.decodeMobileSecurityObject();
    }

    @DisplayName("then string claims are supported")
    @Test
    void testIssueMDocCredentialsWithStringClaim() throws Exception {
        Claim stringClaim1 = buildClaim("foo", "string1", new StringValue("foobar"));
        Claim stringClaim2 = buildClaim("foo", "string2", new StringValue("foobar-foooooo"));

        IssuerSigned mdoc = mDocService.createMDoc(generateDeviceKey(), credentialConfiguration("foo", "eaa-provider"), List.of(stringClaim1, stringClaim2), null);
        List<IssuerSignedItem> items = items(mdoc, "foo");
        assertAll(
                () -> assertEquals("foo", mso(mdoc).getDocType()),
                () -> assertEquals(2, items.size()),
                () -> assertEquals("string1", items.getFirst().getElementIdentifier()),
                () -> assertEquals("foobar", value(items.getFirst())),
                () -> assertEquals("string2", items.getLast().getElementIdentifier()),
                () -> assertEquals("foobar-foooooo", value(items.getLast()))
        );
    }

    private static Claim buildClaim(String path, String key, ClaimValue value) {
        return Claim.builder().path(path).path(key).value(value).build();
    }

    @DisplayName("then boolean claims are supported")
    @Test
    void testIssueMDocCredentialsWithBooleanClaim() throws Exception {
        Claim claim = buildClaim("foo", "cool-id", new BooleanValue(true));

        IssuerSigned mdoc = mDocService.createMDoc(generateDeviceKey(), credentialConfiguration("foo", "eaa-provider"), List.of(claim), null);
        assertAll(
                () -> assertEquals("cool-id", items(mdoc, "foo").getFirst().getElementIdentifier()),
                () -> assertEquals(true, value(items(mdoc, "foo").getFirst())),
                () -> assertEquals(1, items(mdoc, "foo").size())
        );
    }

    @DisplayName("then number claims are supported")
    @Test
    void testIssueMDocCredentialsWithNumberClaim() throws Exception {
        Claim claim = buildClaim("foo", "my-id", new NumberValue(12L));

        IssuerSigned mdoc = mDocService.createMDoc(generateDeviceKey(), credentialConfiguration("foo", "eaa-provider"), List.of(claim), null);
        assertAll(
                () -> assertEquals("my-id", items(mdoc, "foo").getFirst().getElementIdentifier()),
                () -> assertEquals(12L, value(items(mdoc, "foo").getFirst())),
                () -> assertEquals(1, items(mdoc, "foo").size())
        );
    }

    @DisplayName("then date claims are supported")
    @Test
    void testIssueMDocCredentialsDatetimeAndFullDate() throws Exception {
        Claim fullDateClaim = buildClaim("foo", "fulldato", new FullDateValue(LocalDate.of(2026, 11, 26)));
        ZonedDateTime dateTime = ZonedDateTime.of(2026, 11, 26, 13, 37, 42, 123_000_000, ZoneOffset.ofHours(1));
        Claim datetimeClaim = buildClaim("foo", "datotid", new DateTimeValue(dateTime));

        IssuerSigned mdoc = mDocService.createMDoc(generateDeviceKey(), credentialConfiguration("foo", "eaa-provider"), List.of(fullDateClaim, datetimeClaim), null);
        assertAll(
                () -> assertEquals("fulldato", items(mdoc, "foo").getFirst().getElementIdentifier()),
                () -> assertEquals("1004:2026-11-26", value(items(mdoc, "foo").getFirst())),
                () -> assertEquals("datotid", items(mdoc, "foo").getLast().getElementIdentifier()),
                () -> assertEquals("0:2026-11-26T12:37:42Z", value(items(mdoc, "foo").getLast())),
                () -> assertEquals(2, items(mdoc, "foo").size())
        );
    }

    @DisplayName("then binary claims are supported")
    @Test
    void testIssueMDocCredentialsWithBinaryClaim() throws Exception {
        String img = "base64encodedstring";
        Claim claim = buildClaim("namespace", "portrait", new BinaryValue(img));

        IssuerSigned mdoc = mDocService.createMDoc(generateDeviceKey(), credentialConfiguration("namespace", "eaa-provider"), List.of(claim), null);
        assertAll(
                () -> assertEquals("portrait", items(mdoc, "namespace").getFirst().getElementIdentifier()),
                () -> assertArrayEquals(Base64.getDecoder().decode(img), (byte[]) value(items(mdoc, "namespace").getFirst())),
                () -> assertEquals(1, items(mdoc, "namespace").size())
        );
    }

    @DisplayName("then list claims are supported")
    @Test
    void testIssueMDocCredentialsList() throws Exception {
        List<ClaimValue> list = List.of(new StringValue("one"), new StringValue("two"), new StringValue("three"));
        Claim claim = buildClaim("mynamespace", "mylist", new ListValue(list));

        IssuerSigned mdoc = mDocService.createMDoc(generateDeviceKey(), credentialConfiguration("mydoctype", "eaa-provider"), List.of(claim), null);
        assertAll(
                () -> assertEquals("mydoctype", mso(mdoc).getDocType()),
                () -> assertEquals("mylist", items(mdoc, "mynamespace").getFirst().getElementIdentifier()),
                () -> assertEquals(List.of("one", "two", "three"), value(items(mdoc, "mynamespace").getFirst())),
                () -> assertEquals(1, items(mdoc, "mynamespace").size())
        );
    }

    @DisplayName("then map claims are supported")
    @Test
    void testIssueMDocCredentialsMap() throws Exception {
        Map<String, ClaimValue> map = Map.of(
                "key1", new StringValue("value1"),
                "key2", new FullDateValue(LocalDate.of(2026, 11, 26))
        );
        Claim claim = buildClaim("mynamespace", "mymap", new MapValue(map));

        IssuerSigned mdoc = mDocService.createMDoc(generateDeviceKey(), credentialConfiguration("mydoctype", "eaa-provider"), List.of(claim), null);
        assertAll(
                () -> assertEquals("mymap", items(mdoc, "mynamespace").getFirst().getElementIdentifier()),
                () -> assertEquals(Map.of("key1", "value1", "key2", "1004:2026-11-26"), value(items(mdoc, "mynamespace").getFirst())),
                () -> assertEquals(1, items(mdoc, "mynamespace").size())
        );
    }

    @DisplayName("then validity info is supported")
    @Test
    void testIssueMDocWithValidity() throws Exception {
        ExtendedCredentialConfiguration credentialConfiguration = credentialConfiguration("foo", "eaa-provider");
        credentialConfiguration.getCredentialIssuerContext().setValidityDays(42);
        Claim claim = buildClaim("foo", "cool-id", new BooleanValue(true));
        IssuerSigned mdoc = mDocService.createMDoc(generateDeviceKey(), credentialConfiguration, List.of(claim), null);
        java.time.Instant now = java.time.Instant.now();
        assertAll(
                () -> assertEquals(
                        now.plus(42, ChronoUnit.DAYS).getEpochSecond(),
                        mso(mdoc).getValidityInfo().getValidUntil().getEpochSeconds(),
                        24 * 60 * 60),
                () -> assertEquals(
                        now.getEpochSecond(),
                        mso(mdoc).getValidityInfo().getValidFrom().getEpochSeconds(),
                        24 * 60 * 60)
        );
    }

    @DisplayName("then token status list claims are supported")
    @Test
    void testIssueMDocCredentialsWithStatus() throws Exception {
        Claim stringClaim1 = buildClaim("foo", "string1", new StringValue("foobar"));

        IssuerSigned mdoc = mDocService.createMDoc(
                generateDeviceKey(),
                credentialConfiguration("foo", "eaa-provider"),
                List.of(stringClaim1),
                CredentialStatus.create(67, URI.create("https://junit.status.eidas2sandkasse.dev/lists/1")));
        Status status = mso(mdoc).getStatus();
        assertAll(
                () -> assertNotNull(status),
                () -> assertNotNull(status.getStatusList()),
                () -> assertEquals("https://junit.status.eidas2sandkasse.dev/lists/1", MDocStatusBridge.extractUri(status)),
                () -> assertEquals(67, MDocStatusBridge.extractIndex(status))
        );
    }

    @DisplayName("then an mdoc is signed and can be encoded and decoded")
    @Test
    void testIssuedMDocIsSignedByIssuerAndSurvivesEncoding() throws Exception {
        Claim claim = buildClaim("foo", "string1", new StringValue("foobar"));
        IssuerSigned mdoc = mDocService.createMDoc(generateDeviceKey(), credentialConfiguration("foo", "eaa-provider"), List.of(claim), null);
        X509Certificate issuerCertificate = (X509Certificate) keystoreManager.getKeyProvider("eaa-provider").certificate();

        IssuerSigned decoded = MDocTestSupport.decode(Base64.getUrlDecoder().decode(mDocService.encode(mdoc)));
        assertAll(
                () -> assertTrue(MDocTestSupport.verifySignature(decoded, issuerCertificate)),
                () -> assertEquals("foobar", value(items(decoded, "foo").getFirst())),
                () -> assertEquals("foo", mso(decoded).getDocType())
        );
    }

    static Stream<Arguments> deviceKeyCurves() {
        return Stream.of(
                Arguments.of(Curve.P_256, Cose.EllipticCurves.P_256),
                Arguments.of(Curve.P_384, Cose.EllipticCurves.P_384),
                Arguments.of(Curve.P_521, Cose.EllipticCurves.P_521));
    }


    @DisplayName("then selected device key curves are supported")
    @ParameterizedTest
    @MethodSource("deviceKeyCurves")
    void testIssueMDocBindsDeviceKeyWithCurve(Curve curve, int coseCurve) throws Exception {
        ECKey deviceKey = new ECKeyGenerator(curve).generate();
        Claim claim = buildClaim("foo", "string1", new StringValue("foobar"));

        IssuerSigned mdoc = mDocService.createMDoc(deviceKey, credentialConfiguration("foo", "eaa-provider"), List.of(claim), null);
        CoseKey coseKey = mso(mdoc).getDeviceKeyInfo().getDeviceKey();
        assertAll(
                () -> assertEquals(Cose.KeyTypes.EC2, coseKey.getKty()),
                () -> assertEquals(coseCurve, coseKey.getCrv()),
                () -> assertArrayEquals(deviceKey.getX().decode(), coseKey.getX()),
                () -> assertArrayEquals(deviceKey.getY().decode(), coseKey.getY())
        );
    }

    @DisplayName("then unsupported device key curves are rejected")
    @Test
    void testIssueMDocFailsForUnsupportedDeviceKeyCurve() throws Exception {
        ECKey deviceKey = new ECKeyGenerator(Curve.SECP256K1).provider(Security.getProvider("BC")).generate();
        Claim claim = buildClaim("foo", "string1", new StringValue("foobar"));

        assertThrows(IssuerServerException.class,
                () -> mDocService.createMDoc(deviceKey, credentialConfiguration("foo", "eaa-provider"), List.of(claim), null));
    }

    private static ECKey generateDeviceKey() throws JOSEException {
        return new ECKeyGenerator(Curve.P_256).generate();
    }

}
