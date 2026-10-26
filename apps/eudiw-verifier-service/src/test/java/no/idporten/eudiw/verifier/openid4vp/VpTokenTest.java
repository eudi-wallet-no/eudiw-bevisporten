package no.idporten.eudiw.verifier.openid4vp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("When formatting VpToken for audit logs")
class VpTokenTest {

    @Test
    @DisplayName("When multiple verifiable presentations" +
            " are present in vp token, then they are rendered on one line with credential ids")
    void rendersCredentialIdsAndValuesOnOneLine() {
        VpToken vpToken = new VpToken(Map.of(
                "credential-1", List.of(
                        new VerifiablePresentation("token-1"),
                        new VerifiablePresentation("token-2")),
                "credential-2", List.of(new VerifiablePresentation("token-3"))
        ));

        String logged = vpToken.auditLogFormatted();
        assertTrue(logged.contains("\"credential-1\":[\"token-1\",\"token-2\"]"));
        assertTrue(logged.contains("\"credential-2\":[\"token-3\"]"));
        assertEquals("token-1", new VerifiablePresentation("token-1").value());
    }
}
