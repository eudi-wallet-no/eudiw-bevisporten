package no.idporten.eudiw.verifier.openid4vp;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.JSONObjectUtils;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlCredentialQuery;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test data and helpers for the credential verifiers. The launchpad samples are PIDs collected from wallets at the
 * EUDI launchpad, one directory per issuer, and launchpad-sessions.json holds the session data of their authorization
 * requests, taken from the verifier audit log.
 */
final class VerifierTestSupport {

    static final String PID_DOC_TYPE = "eu.europa.ec.eudi.pid.1";

    private VerifierTestSupport() {
    }

    static void assertHolderBindingFails(Executable verification) {
        VerificationException exception = assertThrows(VerificationException.class, verification);
        assertEquals("Invalid vp_token. Holder binding failed.", exception.getErrorDescription());
    }

    static Clock clockAt(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    static VerificationTransaction transaction(String nonce, String audience) {
        VerificationTransaction transaction = new VerificationTransaction();
        transaction.setNonce(nonce);
        transaction.setAudience(audience);
        return transaction;
    }

    @SuppressWarnings("unchecked")
    static VerificationTransaction sessionOf(String sample) throws Exception {
        Map<String, Object> session = (Map<String, Object>) JSONObjectUtils.parse(read("launchpad-sessions.json")).get(sample);
        VerificationTransaction transaction = transaction((String) session.get("nonce"), (String) session.get("client_id"));
        transaction.setRequest(Map.of("response_uri", session.get("response_uri")));
        transaction.setEncryptionKey(JWK.parse((Map<String, Object>) session.get("encryption_jwk")));
        return transaction;
    }

    static DcqlCredentialQuery sdJwtQuery(boolean requireHolderBinding) {
        return query("dc+sd-jwt", requireHolderBinding);
    }

    static DcqlCredentialQuery mdocQuery(boolean requireHolderBinding) {
        return query("mso_mdoc", requireHolderBinding);
    }

    private static DcqlCredentialQuery query(String format, boolean requireHolderBinding) {
        DcqlCredentialQuery query = new DcqlCredentialQuery();
        query.setId("pid");
        query.setFormat(format);
        query.setRequireCryptographicHolderBinding(requireHolderBinding);
        return query;
    }

    static String read(String resource) throws IOException, URISyntaxException {
        return Files.readString(resource(resource)).trim();
    }

    static Stream<String> samples(String directory, String extension) throws IOException, URISyntaxException {
        Path root = resource(directory);
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(file -> file.toString().endsWith(extension))
                    .map(file -> directory + "/" + root.relativize(file).toString().replace('\\', '/'))
                    .sorted()
                    .toList()
                    .stream();
        }
    }

    private static Path resource(String name) throws URISyntaxException {
        return Path.of(Objects.requireNonNull(VerifierTestSupport.class.getClassLoader().getResource(name), name).toURI());
    }
}
