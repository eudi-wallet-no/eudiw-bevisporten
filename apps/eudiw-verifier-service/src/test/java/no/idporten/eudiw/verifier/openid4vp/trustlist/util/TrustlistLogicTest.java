package no.idporten.eudiw.verifier.openid4vp.trustlist.util;

import com.nimbusds.jose.util.X509CertUtils;
import no.idporten.eudiw.verifier.config.TrustlistsProperties;
import no.idporten.eudiw.verifier.openid4vp.trustlist.etsi602.Trustlist;
import no.idporten.eudiw.verifier.openid4vp.trustlist.etsi602.pojo.LoTEResponse;
import no.idporten.eudiw.verifier.openid4vp.trustlist.etsi612.LoTE;
import no.idporten.eudiw.verifier.testdata.Certificates;
import no.idporten.eudiw.verifier.testdata.TrustlistTestdata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import java.net.URI;
import java.security.cert.X509Certificate;

@SpringBootTest
@ActiveProfiles("junit")
class TrustlistLogicTest {

    private static final Logger log = LoggerFactory.getLogger(TrustlistLogicTest.class);
    private static URI XMLTRUSTLISTURL =URI.create("https://tillitsliste.eidas2sandkasse.dev/no_eidas2sandkasse_dev_tsl.xtsl");
    private static URI JSONTRUSTLISTURL = URI.create("https://tillitsliste.eidas2sandkasse.dev/no_eidas2sandkasse_dev_pid.jws");

    private TrustlistLogic trustlistLogic;

    private MockRestServiceServer mockServer;

    @Autowired
    private TrustlistsProperties trustlistProperties;

    private final TrustlistTestdata trustlistTestdata = new TrustlistTestdata();
    private final Certificates certificates = new Certificates();

    public TrustlistLogicTest() {
    }

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restclient = builder.build();
        trustlistLogic = new TrustlistLogic(restclient, trustlistProperties);
    }


    @Test
    @DisplayName("that util method of conecting to trustlist takes the uri and returns LoTE")
    void connectToTrustlistReturnsLoteWhenUriIsCorrect() throws Exception {

        mockServer.expect(requestTo(XMLTRUSTLISTURL))
                .andRespond(withSuccess(trustlistTestdata.getXmlTrustlist(), MediaType.parseMediaType("application/vnd.etsi.tsl+xml")));

        LoTE lote = (LoTE) trustlistLogic.connectToTrustlist(XMLTRUSTLISTURL);
        assertNotNull(lote);
        assertEquals("DIGITALISERINGSDIREKTORATET", lote.schemeInformation().schemeName().names().getFirst().getValue());
    }

    @Test
    @DisplayName("that util method of conecting to trustlist takes the uri and returns Trustlist")
    void connectToTrustlistReturnsLotResponseeWhenUriIsCorrect() throws Exception {

        mockServer.expect(requestTo(JSONTRUSTLISTURL))
                .andRespond(withSuccess(trustlistTestdata.getJsonTrustlist(), MediaType.parseMediaType("application/jose+json")));

        LoTEResponse lote = (LoTEResponse) trustlistLogic.connectToTrustlist(JSONTRUSTLISTURL);
        assertNotNull(lote);
        log.info("Trustlist: {}", lote);
        assertEquals("Tillitsliste for Personal Identification Data tilbydere i eidas2sandkasse i test", lote.lote().schemeInformation().schemeName().getFirst().getLocalisedValue());
    }



    @Test
    @DisplayName("that trustlist has expected content")
    void trustlistHasExpectedContent() throws Exception {
        mockServer.expect(requestTo(XMLTRUSTLISTURL))
                .andRespond(withSuccess(trustlistTestdata.getXmlTrustlist(), MediaType.parseMediaType("application/vnd.etsi.tsl+xml")));

        LoTE lote = (LoTE) trustlistLogic.connectToTrustlist(XMLTRUSTLISTURL);
        assertAll(
                () -> assertNotNull(lote),
                () -> assertEquals("DIGITALISERINGSDIREKTORATET", lote.schemeInformation().schemeName().names().getFirst().getValue()),
                () -> assertEquals("https://docs.digdir.no/docs/lommebok/lommebok_om.html", lote.schemeInformation().informationUris().uris().getFirst().getValue()),
                () -> assertEquals(certificates.getBevisportenCertificate(), lote.serviceProviderList().trustServiceProviders().getFirst().services().services().getFirst().serviceInformation().serviceDigitalIdentity().digitalIds().get(1).getCert()),
                () -> assertEquals(certificates.trustlistCertificateInsideSignatureField(), lote.signature().keyInfo().x509Data().x509Certificate()),
                () -> assertEquals("AQYIX13utrL7PoajrKm53SN7x0VTb49kaHCt3CzYNAY=", lote.signature().signedInfo().reference().digestValue())
        );

    }

    @Test
    @DisplayName("that trustlists from config properties are read in, and that the list of trustlists are used " +
            "when checking for matching entry in trustlist")
    void checkIfUrlsFromTrustlistPropertiesAreIteratedOverAndUsedWhenSerarhingForMatchingEntry() throws Exception {
        mockServer.expect(requestTo(XMLTRUSTLISTURL))
                .andRespond(withSuccess(trustlistTestdata.getXmlTrustlist(), MediaType.parseMediaType("application/vnd.etsi.tsl+xml")));
        X509Certificate cert = X509CertUtils.parse("-----BEGIN CERTIFICATE-----" + certificates.getBevisportenCertificate()+ "-----END CERTIFICATE-----");
        boolean result = trustlistLogic.checkIfCertificateFromJwsHeaderIsOnTrustlist(cert);
        assertTrue(result);
    }

    @Test
    @DisplayName("that certificate down the list is checked when first entry is not matching")
    void checkIfCertificateDownTheListIsCheckedAgainst() throws Exception {
        mockServer.expect(requestTo(XMLTRUSTLISTURL))
                .andRespond(withSuccess(trustlistTestdata.getXmlTrustlist(), MediaType.parseMediaType("application/vnd.etsi.tsl+xml")));
        X509Certificate cert = X509CertUtils.parse("-----BEGIN CERTIFICATE-----" + certificates.getSecondBevisporten()+ "-----END CERTIFICATE-----");
        boolean result = trustlistLogic.checkIfCertificateFromJwsHeaderIsOnTrustlist(cert);
        assertTrue(result);
    }

    @Test
    @DisplayName("that if certificate from vp token response from wallet is not on trustlist, result is false")
    void resultIsFalseWhenCertIsNotOnTrustlist() throws Exception {
        mockServer.expect(requestTo(XMLTRUSTLISTURL))
                .andRespond(withSuccess(trustlistTestdata.getXmlTrustlist(), MediaType.parseMediaType("application/vnd.etsi.tsl+xml")));
        mockServer.expect(requestTo(XMLTRUSTLISTURL))
                .andRespond(withSuccess(trustlistTestdata.getXmlTrustlist(), MediaType.parseMediaType("application/vnd.etsi.tsl+xml")));
        mockServer.expect(requestTo(JSONTRUSTLISTURL))
                .andRespond(withSuccess(trustlistTestdata.getJsonTrustlist(), MediaType.parseMediaType("application/jose+json")));
        mockServer.expect(requestTo(JSONTRUSTLISTURL))
                .andRespond(withSuccess(trustlistTestdata.getJsonTrustlist(), MediaType.parseMediaType("application/jose+json")));
        X509Certificate cert = X509CertUtils.parse("-----BEGIN CERTIFICATE-----" + certificates.certificateThatIsNotOnTrustlist()+ "-----END CERTIFICATE-----");
        boolean result = trustlistLogic.checkIfCertificateFromJwsHeaderIsOnTrustlist(cert);
        assertFalse(result);
    }
}
