package no.idporten.eudiw.verifier.trustlist;

import com.nimbusds.jose.util.X509CertUtils;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import no.idporten.eudiw.verifier.trustlist.etsi602.LoTEJson;
import no.idporten.eudiw.verifier.trustlist.etsi602.ValueCertificate;
import no.idporten.eudiw.verifier.trustlist.etsi602xml.LoTEXml602;
import no.idporten.eudiw.verifier.trustlist.etsi612.LoTEXml;
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


import static no.idporten.eudiw.verifier.testdata.TrustlistTestdata.getJsonTrustlist;
import static no.idporten.eudiw.verifier.testdata.TrustlistTestdata.getXmlTrustlist;
import static no.idporten.eudiw.verifier.testdata.TrustlistTestdata.getJsonInvalidCertList;
import static no.idporten.eudiw.verifier.testdata.TrustlistTestdata.getXmlWebuildPidTrustlist;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.net.URI;
import java.security.cert.X509Certificate;

@SpringBootTest
@ActiveProfiles("junit")
class TrustlistServiceTest {

    private static final Logger log = LoggerFactory.getLogger(TrustlistServiceTest.class);
    public static final URI XMLTRUSTLISTURL = URI.create("https://tillitsliste.eidas2sandkasse.dev/no_eidas2sandkasse_dev_tsl.xtsl");
    public static final URI JSONTRUSTLISTURL = URI.create("https://tillitsliste.eidas2sandkasse.dev/no_eidas2sandkasse_dev_pid.jws");
    public static final URI JWTTRUSTLISTURL = URI.create("https://trustlist.webuild.jwt");
    public static final URI XMLWEBUILDPIDTRUSTLISTURL = URI.create("https://tl-api.dev.idunion.info/api/v1/xPnOSuTc/etsi/tl.xml");
    public static final TrustlistReference XMLTRUSTLIST = new TrustlistReference(XMLTRUSTLISTURL, TrustlistFormat.ETSI_612_XML);
    public static final TrustlistReference JSONTRUSTLIST = new TrustlistReference(JSONTRUSTLISTURL, TrustlistFormat.ETSI_602);
    public static final TrustlistReference JWTTRUSTLIST = new TrustlistReference(JWTTRUSTLISTURL, TrustlistFormat.ETSI_602);
    public static final TrustlistReference XMLWEBUILDPIDTRUSTLIST = new TrustlistReference(XMLWEBUILDPIDTRUSTLISTURL, TrustlistFormat.ETSI_602);
    public static final String APPLICATION_JOSE_JSON = "application/jose+json";
    public static final String APPLICATION_ETSI_TSL_XML = "application/vnd.etsi.tsl+xml";
    public static final String BEGIN_CERTIFICATE = "-----BEGIN CERTIFICATE-----";
    public static final String END_CERTIFICATE = "-----END CERTIFICATE-----";

    private TrustlistService trustlistService;

    private MockRestServiceServer mockServer;

    @Autowired
    private TrustlistsProperties trustlistProperties;

    private final TrustlistTestdata trustlistTestdata;
    private final Certificates certificates = new Certificates();

    public TrustlistServiceTest() {
        try {
            trustlistTestdata = new TrustlistTestdata();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restclient = builder.build();
        trustlistService = new TrustlistService(restclient, trustlistProperties);
    }


    @Test
    @DisplayName("that method of conecting to trustlist takes the uri and returns LoTE from ETSI TS 119 612")
    void connectToTrustlistReturnsLoteWhenUriIsCorrect612() {

        mockServer.expect(requestTo(XMLTRUSTLISTURL))
                .andRespond(withSuccess(getXmlTrustlist(), MediaType.parseMediaType(APPLICATION_ETSI_TSL_XML)));

        LoTEXml lote = (LoTEXml) trustlistService.connectToTrustlist(XMLTRUSTLIST);
        assertNotNull(lote);
        assertEquals("DIGITALISERINGSDIREKTORATET", lote.schemeInformation().schemeName().names().getFirst().getValue());
    }

    @Test
    @DisplayName("that method of connecting to trustlist takes the uri and returns LoTE from ETSI TS 119 602")
    void connectToTrustlistReturnsLotResponseeWhenUriIsCorrect602() {

        mockServer.expect(requestTo(JSONTRUSTLISTURL))
                .andRespond(withSuccess(getJsonTrustlist(), MediaType.parseMediaType(APPLICATION_JOSE_JSON)));

        LoTEJson lote = (LoTEJson) trustlistService.connectToTrustlist(JSONTRUSTLIST);
        assertNotNull(lote);
        assertEquals("Tillitsliste for Personal Identification Data tilbydere i eidas2sandkasse i test", lote.lote().schemeInformation().schemeName().getFirst().getLocalisedValue());
    }

    @Test
    @DisplayName("that a jwt trustlist URL is parsed as an ETSI TS 119 602 trustlist")
    void connectToTrustlistReturnsLoteWhenJwtUriIsCorrect602() {
        mockServer.expect(requestTo(JWTTRUSTLISTURL))
                .andRespond(withSuccess(getJsonTrustlist(), MediaType.parseMediaType(APPLICATION_JOSE_JSON)));

        LoTEJson lote = (LoTEJson) trustlistService.connectToTrustlist(JWTTRUSTLIST);

        assertEquals("Tillitsliste for Personal Identification Data tilbydere i eidas2sandkasse i test",
                lote.lote().schemeInformation().schemeName().getFirst().getLocalisedValue());
    }

    @Test
    @DisplayName("that a certificate can be checked against a jwt trustlist")
    void checkCertificateAgainstJwtTrustlist() {
        mockServer.expect(requestTo(JWTTRUSTLISTURL))
                .andRespond(withSuccess(getJsonTrustlist(), MediaType.parseMediaType(APPLICATION_JOSE_JSON)));
        X509Certificate cert = X509CertUtils.parse(BEGIN_CERTIFICATE
                + certificates.trustlistCertificatePIDSecondOnList() + END_CERTIFICATE);

        ValidationStatus result = trustlistService.checkIfCertificateFromJwsHeaderIsOnTrustlist(cert, JWTTRUSTLIST);

        assertEquals(ValidationStatus.VALID, result);
    }


    @Test
    @DisplayName("that 612 trustlist has expected content")
    void trustlistHasExpectedContent() {
        mockServer.expect(requestTo(XMLTRUSTLISTURL))
                .andRespond(withSuccess(getXmlTrustlist(), MediaType.parseMediaType(APPLICATION_ETSI_TSL_XML)));

        LoTEXml lote = (LoTEXml) trustlistService.connectToTrustlist(XMLTRUSTLIST);
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
    @DisplayName("that 612 trustlists from config properties are read in, and that the list of trustlists are used " +
            "when checking for matching entry in trustlist")
    void checkIfUrlsFromTrustlistPropertiesAreIteratedOverAndUsedWhenSerarhingForMatchingEntry() {
        mockServer.expect(requestTo(XMLTRUSTLISTURL))
                .andRespond(withSuccess(getXmlTrustlist(), MediaType.parseMediaType(APPLICATION_ETSI_TSL_XML)));
        X509Certificate cert = X509CertUtils.parse(BEGIN_CERTIFICATE + certificates.getBevisportenCertificate() + END_CERTIFICATE);
        ValidationStatus result = trustlistService.checkIfCertificateFromJwsHeaderIsOnTrustlist(cert);
        assertEquals(ValidationStatus.VALID, result);
    }

    @Test
    @DisplayName("that certificate down the list on 612 is checked when first entry is not matching")
    void checkIfCertificateDownTheListIsCheckedAgainst612() {
        mockServer.expect(requestTo(XMLTRUSTLISTURL))
                .andRespond(withSuccess(getXmlTrustlist(), MediaType.parseMediaType(APPLICATION_ETSI_TSL_XML)));
        X509Certificate cert = X509CertUtils.parse(BEGIN_CERTIFICATE + certificates.getSecondBevisporten() + END_CERTIFICATE);
        ValidationStatus result = trustlistService.checkIfCertificateFromJwsHeaderIsOnTrustlist(cert);
        assertEquals(ValidationStatus.VALID, result);
    }

    @Test
    @DisplayName("that certificate down the list on 602 is checked when first entry is not matching")
    void checkIfCertificateDownTheListIsCheckedAgainst602() {
        mockServer.expect(requestTo(XMLTRUSTLISTURL))
                .andRespond(withSuccess(getXmlTrustlist(), MediaType.parseMediaType(APPLICATION_ETSI_TSL_XML)));
        mockServer.expect(requestTo(JSONTRUSTLISTURL))
                .andRespond(withSuccess(getJsonTrustlist(), MediaType.parseMediaType(APPLICATION_JOSE_JSON)));
        ValueCertificate valueCertificate = new ValueCertificate(certificates.trustlistCertificatePIDSecondOnList());
        ValidationStatus result = trustlistService.checkIfCertificateFromJwsHeaderIsOnTrustlist(valueCertificate.getCertificateAsX509Object());
        assertEquals(ValidationStatus.VALID, result);
    }

    @Test
    @DisplayName("that if certificate from vp token response from wallet is not on trustlist, result is false")
    void resultIsFalseWhenCertIsNotOnTrustlist() {
        mockServer.expect(requestTo(XMLTRUSTLISTURL))
                .andRespond(withSuccess(getXmlTrustlist(), MediaType.parseMediaType(APPLICATION_ETSI_TSL_XML)));
        mockServer.expect(requestTo(JSONTRUSTLISTURL))
                .andRespond(withSuccess(getJsonTrustlist(), MediaType.parseMediaType(APPLICATION_JOSE_JSON)));
        mockServer.expect(requestTo(JWTTRUSTLISTURL))
                .andRespond(withSuccess(getJsonTrustlist(), MediaType.parseMediaType(APPLICATION_JOSE_JSON)));
        X509Certificate cert = X509CertUtils.parse(BEGIN_CERTIFICATE + certificates.certificateThatIsNotOnTrustlist() + END_CERTIFICATE);
        ValidationStatus result = trustlistService.checkIfCertificateFromJwsHeaderIsOnTrustlist(cert);
        assertEquals(ValidationStatus.INVALID, result);
    }


    @Test
    @DisplayName("that PID checks trustlist and is valid")
    void pidTrustlistChechValid() {
        mockServer.expect(requestTo(XMLTRUSTLISTURL))
                .andRespond(withSuccess(getXmlTrustlist(), MediaType.parseMediaType(APPLICATION_ETSI_TSL_XML)));
        mockServer.expect(requestTo(JSONTRUSTLISTURL))
                .andRespond(withSuccess(getJsonTrustlist(), MediaType.parseMediaType(APPLICATION_JOSE_JSON)));

        X509Certificate cert = X509CertUtils.parse(BEGIN_CERTIFICATE + certificates.trustlistCertificatePIDSecondOnList() + END_CERTIFICATE);
        ValidationStatus result = trustlistService.checkIfCertificateFromJwsHeaderIsOnTrustlist(cert);
        assertEquals(ValidationStatus.VALID, result);
    }

    @Test
    @DisplayName("that comparing certificates fails when the certificates are different")
    void testCompareCertificatesFailsWhenCertificatesAreDifferent() {
        X509Certificate cert1 = X509CertUtils.parse(BEGIN_CERTIFICATE + certificates.trustlistCertificatePIDFirstOnList() + END_CERTIFICATE);
        X509Certificate cert2 = X509CertUtils.parse(BEGIN_CERTIFICATE + certificates.trustlistCertificatePIDSecondOnList() + END_CERTIFICATE);
        assertFalse(trustlistService.compareCertificates(cert1, cert2));
    }

    @Test
    @DisplayName("that comparing certificates succeeds when the certificates are the same when one is parsed through " +
            "ValueCertificate while other is parsed in test")
    void testCompareCertificatesSucceedsWhenCertificatesAreTheSame() {
        ValueCertificate certificate = new ValueCertificate(certificates.trustlistCertificatePIDFirstOnList());
        X509Certificate cert2 = X509CertUtils.parse(BEGIN_CERTIFICATE + certificates.trustlistCertificatePIDFirstOnList() + END_CERTIFICATE);
        assertTrue(trustlistService.compareCertificates(certificate.getCertificateAsX509Object(), cert2));
    }

    @Test
    @DisplayName("Invalid cert on trustlist throws error in ValueCertificate")
    void invalidCertThrowsErrorInValueCertificate() {
        mockServer.expect(requestTo(XMLTRUSTLISTURL))
                .andRespond(withSuccess(getXmlTrustlist(), MediaType.parseMediaType(APPLICATION_ETSI_TSL_XML)));
        mockServer.expect(requestTo(JSONTRUSTLISTURL))
                .andRespond(withSuccess(getJsonInvalidCertList(), MediaType.parseMediaType(APPLICATION_JOSE_JSON)));
        X509Certificate validCert = X509CertUtils.parse(BEGIN_CERTIFICATE + certificates.certificateThatIsNotOnTrustlist() + END_CERTIFICATE);
        assertThrows(VerificationException.class, () -> trustlistService.checkIfCertificateFromJwsHeaderIsOnTrustlist(validCert));
    }

    @Test
    @DisplayName("that xml is parsed correctly into xml lote object")
    void testXmlListMapping() {
        LoTEXml lote = trustlistService.xmlListMapping(getXmlTrustlist());
        assertNotNull(lote);
        assertEquals("DIGITALISERINGSDIREKTORATET", lote.schemeInformation().schemeName().names().getFirst().getValue());
        assertEquals("https://docs.digdir.no/docs/lommebok/lommebok_om.html", lote.schemeInformation().informationUris().uris().getFirst().getValue());
    }

    @Test
    @DisplayName("that json is parsed correctly into json lote object")
    void testJsonListMapping() {
        LoTEJson lote = trustlistService.jsonListMapping(getJsonTrustlist());
        assertNotNull(lote);
        assertEquals("Tillitsliste for Personal Identification Data tilbydere i eidas2sandkasse i test", lote.lote().schemeInformation().schemeName().getFirst().getLocalisedValue());
    }

    @Test
    @DisplayName("testCheckJSON602")
    void testCheckJSON602() {
        mockServer.expect(requestTo(JSONTRUSTLISTURL))
                .andRespond(withSuccess(getJsonTrustlist(), MediaType.parseMediaType(APPLICATION_JOSE_JSON)));
        ValueCertificate certificate = new ValueCertificate(certificates.trustlistCertificatePIDSecondOnList());
        X509Certificate cert = certificate.getCertificateAsX509Object();
        boolean check = trustlistService.checkJson602(JSONTRUSTLISTURL, cert);
        assertTrue(check);
    }


    @Test
    @DisplayName("testCheckXML612")
    void testCheckXML612() {
        mockServer.expect(requestTo(XMLTRUSTLISTURL))
                .andRespond(withSuccess(getXmlTrustlist(), MediaType.parseMediaType(APPLICATION_ETSI_TSL_XML)));

        ValueCertificate certificate = new ValueCertificate(certificates.getSecondBevisporten());
        X509Certificate cert = certificate.getCertificateAsX509Object();
        boolean check = trustlistService.checkXml612(XMLTRUSTLISTURL, cert);
        assertTrue(check);
    }

    @Test
    @DisplayName("that a real ETSI TS 119 602 XML trustlist URL is parsed as an ETSI TS 119 602 XML trustlist")
    void connectToTrustlistReturnsLoteWhenXmlUriIsCorrect602Xml() {
        mockServer.expect(requestTo(XMLWEBUILDPIDTRUSTLISTURL))
                .andRespond(withSuccess(getXmlWebuildPidTrustlist(), MediaType.parseMediaType(APPLICATION_ETSI_TSL_XML)));

        LoTEXml602 lote = (LoTEXml602) trustlistService.connectToTrustlist(XMLWEBUILDPIDTRUSTLIST);

        assertEquals("EU:EN_WEBUILD - PID Providers", lote.schemeInformation().schemeName().names().getFirst().value());
    }

    @Test
    @DisplayName("that the ETSI TS 119 602 XML trustlist is parsed correctly into its lote object, with all trusted entities")
    void testXml602Mapping() {
        LoTEXml602 lote = trustlistService.xml602Mapping(getXmlWebuildPidTrustlist());
        assertAll(
                () -> assertNotNull(lote),
                () -> assertEquals("EU:EN_WEBUILD - PID Providers", lote.schemeInformation().schemeName().names().getFirst().value()),
                () -> assertEquals("en", lote.schemeInformation().schemeName().names().getFirst().lang()),
                () -> assertEquals(4, lote.trustedEntitiesList().trustedEntities().size())
        );
    }

    @Test
    @DisplayName("testCheckXML602 - certificate for a trusted entity on the XML 602 trustlist is valid")
    void testCheckXML602() {
        mockServer.expect(requestTo(XMLWEBUILDPIDTRUSTLISTURL))
                .andRespond(withSuccess(getXmlWebuildPidTrustlist(), MediaType.parseMediaType(APPLICATION_ETSI_TSL_XML)));

        X509Certificate cert = X509CertUtils.parse(BEGIN_CERTIFICATE + certificates.webuildPidDigdirCertificate() + END_CERTIFICATE);
        boolean check = trustlistService.checkXml602(XMLWEBUILDPIDTRUSTLISTURL, cert);
        assertTrue(check);
    }

    @Test
    @DisplayName("that a certificate not present in the XML 602 trustlist is invalid")
    void testCheckXML602ReturnsFalseWhenCertNotOnTrustlist() {
        mockServer.expect(requestTo(XMLWEBUILDPIDTRUSTLISTURL))
                .andRespond(withSuccess(getXmlWebuildPidTrustlist(), MediaType.parseMediaType(APPLICATION_ETSI_TSL_XML)));

        X509Certificate cert = X509CertUtils.parse(BEGIN_CERTIFICATE + certificates.certificateThatIsNotOnTrustlist() + END_CERTIFICATE);
        boolean check = trustlistService.checkXml602(XMLWEBUILDPIDTRUSTLISTURL, cert);
        assertFalse(check);
    }

    @Test
    @DisplayName("that checkIfCertificateFromJwsHeaderIsOnTrustlist dispatches .xml URLs to the ETSI TS 119 602 XML check")
    void checkCertificateAgainstXml602Trustlist() {
        mockServer.expect(requestTo(XMLWEBUILDPIDTRUSTLISTURL))
                .andRespond(withSuccess(getXmlWebuildPidTrustlist(), MediaType.parseMediaType(APPLICATION_ETSI_TSL_XML)));

        X509Certificate cert = X509CertUtils.parse(BEGIN_CERTIFICATE + certificates.webuildPidDigdirCertificate() + END_CERTIFICATE);
        ValidationStatus result = trustlistService.checkIfCertificateFromJwsHeaderIsOnTrustlist(cert, XMLWEBUILDPIDTRUSTLIST);

        assertEquals(ValidationStatus.VALID, result);
    }
}
