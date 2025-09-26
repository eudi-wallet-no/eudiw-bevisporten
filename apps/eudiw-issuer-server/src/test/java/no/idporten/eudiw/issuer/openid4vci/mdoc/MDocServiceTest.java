package no.idporten.eudiw.issuer.openid4vci.mdoc;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import id.walt.mdoc.dataelement.DataElement;
import id.walt.mdoc.doc.MDoc;
import kotlinx.datetime.Instant;
import no.idporten.eudiw.issuer.claimssource.domain.*;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.security.Security;
import java.time.LocalDate;
import java.time.ZonedDateTime;
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

    @Test
    void testIssueMDocCredentialsWithStringClaim() throws Exception {
        JWK deviceKey = new ECKeyGenerator(Curve.P_256).generate();
        Claim stringClaim1 = Claim.builder().path("foo").path("string1").value(new StringValue("foobar")).build();
        Claim stringClaim2 = Claim.builder().path("foo").path("string2").value(new StringValue("foobar-foooooo")).build();

        MDoc mdoc = mDocService.issueCredentials(
                deviceKey,
                "foo",
                List.of(stringClaim1, stringClaim2));
        assertAll(
                () -> assertNotNull(mdoc),
                () -> assertEquals("foo", mdoc.getMSO().getDocType().getValue()),
                () -> assertEquals("string1", mdoc.getIssuerSignedItems("foo").getFirst().getElementIdentifier().getValue()),
                () -> assertEquals("foobar", mdoc.getIssuerSignedItems("foo").getFirst().getElementValue().getInternalValue()),
                () -> assertEquals("string2", mdoc.getIssuerSignedItems("foo").getLast().getElementIdentifier().getValue()),
                () -> assertEquals("foobar-foooooo", mdoc.getIssuerSignedItems("foo").getLast().getElementValue().getInternalValue())
        );
    }

    @Test
    void testIssueMDocCredentialsDatetimeAndFullDate() throws Exception {
        JWK deviceKey = new ECKeyGenerator(Curve.P_256).generate();
        LocalDate now = LocalDate.now();
        kotlinx.datetime.LocalDate expectedDate = new kotlinx.datetime.LocalDate(
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth());
        Claim fullDateClaim = Claim.builder().path("foo").path("fulldato").value(new FullDateValue(now)).build();
        ZonedDateTime nowTime = ZonedDateTime.now();
        Instant expectedDatetime = Instant.Companion.fromEpochMilliseconds(nowTime.toEpochSecond() * 1000);
        Claim datetimeClaim = Claim.builder().path("foo").path("datotid").value(new DateTimeValue(nowTime)).build();

        MDoc mdoc = mDocService.issueCredentials(
                deviceKey,
                "foo",
                List.of(fullDateClaim, datetimeClaim));
        assertAll(
                () -> assertNotNull(mdoc),
                () -> assertEquals("foo", mdoc.getMSO().getDocType().getValue()),
                () -> assertEquals("fulldato", mdoc.getIssuerSignedItems("foo").getFirst().getElementIdentifier().getValue()),
                () -> assertEquals(expectedDate, mdoc.getIssuerSignedItems("foo").getFirst().getElementValue().getInternalValue()),
                () -> assertEquals("datotid", mdoc.getIssuerSignedItems("foo").getLast().getElementIdentifier().getValue()),
                () -> assertEquals(expectedDatetime, mdoc.getIssuerSignedItems("foo").getLast().getElementValue().getInternalValue())
        );
    }

    @Test
    void testIssueMDocCredentialsList() throws Exception {
        JWK deviceKey = new ECKeyGenerator(Curve.P_256).generate();

        List<ClaimValue> list = List.of(new StringValue("one"), new StringValue("two"), new StringValue("three"));
        Claim claim = Claim.builder().path("path").path("mylist").value(new ListValue(list)).build();

        MDoc mdoc = mDocService.issueCredentials(
                deviceKey,
                "foo",
                List.of(claim));
        assertAll(
                () -> assertNotNull(mdoc),
                () -> assertEquals("foo", mdoc.getMSO().getDocType().getValue()),
                () -> assertEquals("mylist", mdoc.getIssuerSignedItems("foo").getFirst().getElementIdentifier().getValue()),
                () -> assertEquals(list.size(), ((List<DataElement>)mdoc.getIssuerSignedItems("foo").getFirst().getElementValue().getInternalValue()).size())
        );
    }

    @Test
    void testIssueMDocCredentialsMap() throws Exception {
        JWK deviceKey = new ECKeyGenerator(Curve.P_256).generate();

        Map<String,ClaimValue> map = Map.of(
                "key1", new StringValue("value1"),
                "key2", new StringValue("value2")
        );
        Claim claim = Claim.builder().path("path").path("mymap").value(new MapValue(map)).build();

        MDoc mdoc = mDocService.issueCredentials(
                deviceKey,
                "foo",
                List.of(claim));
        assertAll(
                () -> assertNotNull(mdoc),
                () -> assertEquals("foo", mdoc.getMSO().getDocType().getValue()),
                () -> assertEquals("mymap", mdoc.getIssuerSignedItems("foo").getFirst().getElementIdentifier().getValue()),
                () -> assertEquals(map.size(), ((Map<String, DataElement>)mdoc.getIssuerSignedItems("foo").getFirst().getElementValue().getInternalValue()).size())
        );
    }

}
