package no.idporten.eudiw.issuer.credentials.formats;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import id.walt.mdoc.dataelement.DataElement;
import id.walt.mdoc.doc.MDoc;
import id.walt.mdoc.mso.Status;
import kotlin.time.Instant;
import no.idporten.eudiw.issuer.credentials.configurations.CredentialIssuerContext;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.credentials.status.CredentialStatus;
import no.idporten.eudiw.issuer.credentials.types.*;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.security.Security;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("junit")
@SpringBootTest
public class MDocServiceTest {

    @Autowired
    private MDocService mDocService;

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

    @Test
    void testIssueMDocCredentialsWithStringClaim() throws Exception {
        Claim stringClaim1 = buildClaim("foo", "string1", new StringValue("foobar"));
        Claim stringClaim2 = buildClaim("foo", "string2", new StringValue("foobar-foooooo"));

        MDoc mdoc = mDocService.createMDoc(generateDeviceKey(), credentialConfiguration("foo", "eaa-provider"), List.of(stringClaim1, stringClaim2), null);
        assertAll(
                () -> assertNotNull(mdoc),
                () -> assertEquals("foo", mdoc.getMSO().getDocType().getValue()),
                () -> assertEquals("string1", mdoc.getIssuerSignedItems("foo").getFirst().getElementIdentifier().getValue()),
                () -> assertEquals("foobar", mdoc.getIssuerSignedItems("foo").getFirst().getElementValue().getInternalValue()),
                () -> assertEquals("string2", mdoc.getIssuerSignedItems("foo").getLast().getElementIdentifier().getValue()),
                () -> assertEquals("foobar-foooooo", mdoc.getIssuerSignedItems("foo").getLast().getElementValue().getInternalValue()),
                () -> assertEquals(2, mdoc.getIssuerSignedItems("foo").size())
        );
    }

    private static Claim buildClaim(String path, String key, ClaimValue value) {
        return Claim.builder().path(path).path(key).value(value).build();
    }

    @Test
    void testIssueMDocCredentialsWithBooleanClaim() throws Exception {
        Claim claim = buildClaim("foo", "cool-id", new BooleanValue(true));

        MDoc mdoc = mDocService.createMDoc(generateDeviceKey(), credentialConfiguration("foo", "eaa-provider"), List.of(claim), null);
        assertAll(
                () -> assertNotNull(mdoc),
                () -> assertEquals("foo", mdoc.getMSO().getDocType().getValue()),
                () -> assertEquals("cool-id", mdoc.getIssuerSignedItems("foo").getFirst().getElementIdentifier().getValue()),
                () -> assertEquals(true, mdoc.getIssuerSignedItems("foo").getFirst().getElementValue().getInternalValue()),
                () -> assertEquals(1, mdoc.getIssuerSignedItems("foo").size())
        );
    }

    @Test
    void testIssueMDocCredentialsWithNumberClaim() throws Exception {
        Claim claim = buildClaim("foo", "my-id", new NumberValue(12L));

        MDoc mdoc = mDocService.createMDoc(generateDeviceKey(), credentialConfiguration("foo", "eaa-provider"), List.of(claim), null);
        assertAll(
                () -> assertNotNull(mdoc),
                () -> assertEquals("foo", mdoc.getMSO().getDocType().getValue()),
                () -> assertEquals("my-id", mdoc.getIssuerSignedItems("foo").getFirst().getElementIdentifier().getValue()),
                () -> assertEquals(12L, mdoc.getIssuerSignedItems("foo").getFirst().getElementValue().getInternalValue()),
                () -> assertEquals(1, mdoc.getIssuerSignedItems("foo").size())
        );
    }

    @Test
    void testIssueMDocCredentialsDatetimeAndFullDate() throws Exception {
        LocalDate now = LocalDate.now();
        kotlinx.datetime.LocalDate expectedDate = new kotlinx.datetime.LocalDate(
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth());
        Claim fullDateClaim = buildClaim("foo", "fulldato", new FullDateValue(now));
        ZonedDateTime nowTime = ZonedDateTime.now();
        Instant expectedDatetime = Instant.Companion.fromEpochMilliseconds(nowTime.toEpochSecond() * 1000);
        Claim datetimeClaim = buildClaim("foo", "datotid", new DateTimeValue(nowTime));

        MDoc mdoc = mDocService.createMDoc(generateDeviceKey(), credentialConfiguration("foo", "eaa-provider"), List.of(fullDateClaim, datetimeClaim), null);
        assertAll(
                () -> assertNotNull(mdoc),
                () -> assertEquals("foo", mdoc.getMSO().getDocType().getValue()),
                () -> assertEquals("fulldato", mdoc.getIssuerSignedItems("foo").getFirst().getElementIdentifier().getValue()),
                () -> assertEquals(expectedDate, mdoc.getIssuerSignedItems("foo").getFirst().getElementValue().getInternalValue()),
                () -> assertEquals("datotid", mdoc.getIssuerSignedItems("foo").getLast().getElementIdentifier().getValue()),
                () -> assertEquals(expectedDatetime, mdoc.getIssuerSignedItems("foo").getLast().getElementValue().getInternalValue()),
                () -> assertEquals(2, mdoc.getIssuerSignedItems("foo").size())
        );
    }


    @Test
    void testIssueMDocCredentialsWithBinaryClaim() throws Exception {
        String img = "base64encodedstring";
        Claim claim = buildClaim("namespace", "portrait", new BinaryValue(img));

        MDoc mdoc = mDocService.createMDoc(generateDeviceKey(), credentialConfiguration("namespace", "eaa-provider"), List.of(claim), null);
        assertAll(
                () -> assertNotNull(mdoc),
                () -> assertEquals("namespace", mdoc.getMSO().getDocType().getValue()),
                () -> assertEquals("portrait", mdoc.getIssuerSignedItems("namespace").getFirst().getElementIdentifier().getValue()),
                () -> assertInstanceOf(byte[].class, mdoc.getIssuerSignedItems("namespace").getFirst().getElementValue().getInternalValue()),
                () -> assertEquals(1, mdoc.getIssuerSignedItems("namespace").size())
        );
    }

    @Test
    void testIssueMDocCredentialsList() throws Exception {
        List<ClaimValue> list = List.of(new StringValue("one"), new StringValue("two"), new StringValue("three"));
        Claim claim = buildClaim("mynamespace", "mylist", new ListValue(list));

        MDoc mdoc = mDocService.createMDoc(generateDeviceKey(), credentialConfiguration("mydoctype", "eaa-provider"), List.of(claim), null);
        assertAll(
                () -> assertNotNull(mdoc),
                () -> assertEquals("mydoctype", mdoc.getMSO().getDocType().getValue()),
                () -> assertEquals("mylist", mdoc.getIssuerSignedItems("mynamespace").getFirst().getElementIdentifier().getValue()),
                () -> assertEquals(list.size(), ((List<DataElement>) mdoc.getIssuerSignedItems("mynamespace").getFirst().getElementValue().getInternalValue()).size()),
                () -> assertEquals(1, mdoc.getIssuerSignedItems("mynamespace").size())
        );
    }

    @Test
    void testIssueMDocCredentialsMap() throws Exception {
        Map<String, ClaimValue> map = Map.of(
                "key1", new StringValue("value1"),
                "key2", new StringValue("value2")
        );
        Claim claim = buildClaim("mynamespace", "mymap", new MapValue(map));

        MDoc mdoc = mDocService.createMDoc(generateDeviceKey(), credentialConfiguration("mydoctype", "eaa-provider"), List.of(claim), null);
        assertAll(
                () -> assertNotNull(mdoc),
                () -> assertEquals("mydoctype", mdoc.getMSO().getDocType().getValue()),
                () -> assertEquals("mymap", mdoc.getIssuerSignedItems("mynamespace").getFirst().getElementIdentifier().getValue()),
                () -> assertEquals(map.size(), ((Map<String, DataElement>) mdoc.getIssuerSignedItems("mynamespace").getFirst().getElementValue().getInternalValue()).size()),
                () -> assertEquals(1, mdoc.getIssuerSignedItems("mynamespace").size())
        );
    }

    @Test
    void testIssueMDocWithValidity() throws Exception {
        ExtendedCredentialConfiguration credentialConfiguration = credentialConfiguration("foo", "eaa-provider");
        credentialConfiguration.getCredentialIssuerContext().setValidityDays(42);
        Claim claim = buildClaim("foo", "cool-id", new BooleanValue(true));
        MDoc mdoc = mDocService.createMDoc(generateDeviceKey(), credentialConfiguration, List.of(claim), null);
        java.time.Instant now = java.time.Instant.now();
        assertAll(
                () -> assertNotNull(mdoc),
                () -> assertEquals("foo", mdoc.getMSO().getDocType().getValue()),
                () -> assertEquals(1, mdoc.getIssuerSignedItems("foo").size()),
                () -> assertEquals(
                        now.plus(42, ChronoUnit.DAYS).getEpochSecond(),
                        mdoc.getMSO().getValidityInfo().getValidUntil().getValue().getEpochSeconds(),
                        24 * 60 * 60),
                () -> assertEquals(
                        now.getEpochSecond(),
                        mdoc.getMSO().getValidityInfo().getValidFrom().getValue().getEpochSeconds(),
                        24 * 60 * 60)
        );
    }

    @Test
    void testIssueMDocCredentialsWithStatus() throws Exception {
        Claim stringClaim1 = buildClaim("foo", "string1", new StringValue("foobar"));

        MDoc mdoc = mDocService.createMDoc(
                generateDeviceKey(),
                credentialConfiguration("foo", "eaa-provider"),
                List.of(stringClaim1),
                CredentialStatus.create(67, URI.create("https://junit.status.eidas2sandkasse.dev/lists/1")));
        assertAll(
                () -> assertNotNull(mdoc),
                () -> assertEquals("foo", mdoc.getMSO().getDocType().getValue()),
                () -> assertEquals("string1", mdoc.getIssuerSignedItems("foo").getFirst().getElementIdentifier().getValue()),
                () -> assertEquals("foobar", mdoc.getIssuerSignedItems("foo").getFirst().getElementValue().getInternalValue()),
                () -> assertEquals(1, mdoc.getIssuerSignedItems("foo").size())
        );
        Status status = mdoc.getMSO().getStatus();
        assertAll(
                () -> assertNotNull(status),
                () -> assertNotNull(status.getStatusList()),
                () -> assertEquals("https://junit.status.eidas2sandkasse.dev/lists/1", status.getStatusList().getUri()),
                () -> assertEquals(67, MDocStatusBridge.extractIndex(status))
        );

    }

    private static ECKey generateDeviceKey() throws JOSEException {
        return new ECKeyGenerator(Curve.P_256).generate();
    }

}
