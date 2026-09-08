package no.idporten.eudiw.verifier.testdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TrustlistTestdata {

    private static final Logger log = LoggerFactory.getLogger(TrustlistTestdata.class);
    private static String xmlTrustlist;
    private static String jsonTrustlist;
    private static String jsonInvalidCertList;

    public TrustlistTestdata() throws IOException {
        setXmlTrustlist();
        setJsonTrustlist();
        setJsonTrustlistWithInvalidCertificate();
    }

    public void setXmlTrustlist() throws IOException {
        xmlTrustlist = Files.readString(Path.of("src/test/java/no/idporten/eudiw/verifier/testdata/trustlist.xtsl"));
    }

    public static String getXmlTrustlist() {
        return xmlTrustlist;
    }

    public void setJsonTrustlist() throws IOException {
        jsonTrustlist = Files.readString(Path.of("src/test/java/no/idporten/eudiw/verifier/testdata/trustlistPid.jws"));

    }

    public static String getJsonTrustlist() {
        return jsonTrustlist;
    }

    public void setJsonTrustlistWithInvalidCertificate() throws IOException {
        jsonInvalidCertList = Files.readString(Path.of("src/test/java/no/idporten/eudiw/verifier/testdata/trustlistPidWithNonParsableCert.jws"));

    }
    public static String getJsonInvalidCertList() {
        return jsonInvalidCertList;
    }
}