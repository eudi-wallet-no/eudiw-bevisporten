package no.idporten.eudiw.verifier.testdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TrustlistTestdata {

    private static final Logger log = LoggerFactory.getLogger(TrustlistTestdata.class);
    private String trustlist;

    public TrustlistTestdata() {
        setTrustlist();
    }

    public void setTrustlist() {
        try {
            trustlist = Files.readString(Path.of("src/test/java/no/idporten/eudiw/verifier/testdata/trustlist.xtsl"));

        } catch (IOException e) {
            log.info(e.getMessage());
        }
    }

    public String getTrustlist() {
        return trustlist;
    }
}