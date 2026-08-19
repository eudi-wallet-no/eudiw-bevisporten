package no.idporten.eudiw.verifier.testdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TrustlistTestdata {

    private static final Logger log = LoggerFactory.getLogger(TrustlistTestdata.class);
    private String xmlTrustlist;
    private String jsonTrustlist;

    public TrustlistTestdata() {
        setXmlTrustlist();
        setJsonTrustlist();
        log.info("TrustlistTestdata created" + getJsonTrustlist());
    }

    public void setXmlTrustlist() {
        try {
            xmlTrustlist = Files.readString(Path.of("src/test/java/no/idporten/eudiw/verifier/testdata/trustlist.xtsl"));

        } catch (IOException e) {
            log.info(e.getMessage());
        }
    }

    public String getXmlTrustlist() {
        return xmlTrustlist;
    }

    public void setJsonTrustlist() {
        try {
            jsonTrustlist = Files.readString(Path.of("src/test/java/no/idporten/eudiw/verifier/testdata/trustlistPid.jws"));
        } catch (IOException e) {
            log.info(e.getMessage());
        }
    }

    public String getJsonTrustlist() {
        return jsonTrustlist;
    }
}