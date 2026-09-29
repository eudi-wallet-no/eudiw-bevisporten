package no.idporten.eudiw.verifier.testdata;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

public class TrustlistTestdata {

    private static final String XML_TRUSTLIST = read("trustlist.xtsl");
    private static final String JSON_TRUSTLIST = read("trustlistPid.jws");
    private static final String JSON_INVALID_CERT_LIST = read("trustlistPidWithNonParsableCert.jws");
    private static final String XML_WEBUILD_PID_TRUSTLIST = read("trustlistWebuildPid.xml");

    private TrustlistTestdata() {
    }

    public static String getXmlTrustlist() {
        return XML_TRUSTLIST;
    }

    public static String getXmlWebuildPidTrustlist() {
        return XML_WEBUILD_PID_TRUSTLIST;
    }

    public static String getJsonTrustlist() {
        return JSON_TRUSTLIST;
    }

    public static String getJsonInvalidCertList() {
        return JSON_INVALID_CERT_LIST;
    }

    private static String read(String fileName) {
        String resource = "/testdata/" + fileName;
        try (InputStream inputStream = TrustlistTestdata.class.getResourceAsStream(resource)) {
            if (inputStream == null) {
                throw new IllegalStateException("Test data not found on classpath: " + resource);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read test data: " + resource, e);
        }
    }
}
