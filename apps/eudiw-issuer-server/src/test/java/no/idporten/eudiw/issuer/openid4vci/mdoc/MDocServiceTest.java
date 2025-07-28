package no.idporten.eudiw.issuer.openid4vci.mdoc;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import id.walt.mdoc.doc.MDoc;
import no.idporten.eudiw.issuer.claimssource.Claim;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.security.Security;
import java.util.List;

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


    @Test
    void testIssueMDocCredentials() throws Exception {
        JWK deviceKey = new ECKeyGenerator(Curve.P_256).generate();
        MDoc mdoc = mDocService.issueCredentials(
                deviceKey,
                "foo",
                List.of(Claim.builder().path("foo").path("bar").value("foobar").build()));
        assertAll(
                () -> assertNotNull(mdoc),
                () -> assertEquals("foo", mdoc.getMSO().getDocType().getValue()),
                () -> assertEquals("bar", mdoc.getIssuerSignedItems("foo").getFirst().getElementIdentifier().getValue()),
                () -> assertEquals("foobar", mdoc.getIssuerSignedItems("foo").getFirst().getElementValue().getInternalValue())
        );
    }


}
