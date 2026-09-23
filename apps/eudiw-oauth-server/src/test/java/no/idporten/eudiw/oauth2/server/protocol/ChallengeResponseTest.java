package no.idporten.eudiw.oauth2.server.protocol;

import no.idporten.eudiw.oauth2.server.util.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When generating challenge responses")
public class ChallengeResponseTest {

    private static final String ATTESTATION_CHALLENGE = "attestation_challenge";
    private static final String CHALLENGE_VALUE = "challenge-value";

    @Test
    @DisplayName("When building a response, then attestation_challenge is included")
    public void testBuildChallengeResponse() {
        ChallengeResponse challengeResponse = ChallengeResponse.builder()
                .attestationChallenge(CHALLENGE_VALUE)
                .build();
        assertEquals(CHALLENGE_VALUE, challengeResponse.getAttestationChallenge());
    }

    @Test
    @DisplayName("When converting to json object, then attestation_challenge is included")
    public void testChallengeResponseJsonObject() {
        Map<String, Object> jsonObject = ChallengeResponse.builder()
                .attestationChallenge(CHALLENGE_VALUE)
                .build()
                .toJsonObject();
        assertEquals(CHALLENGE_VALUE, jsonObject.get(ATTESTATION_CHALLENGE));
        assertEquals(1, jsonObject.size());
    }

    @Test
    @DisplayName("When creating audit data, then attestation_challenge is included")
    public void testAuditData() {
        ChallengeResponse challengeResponse = ChallengeResponse.builder()
                .attestationChallenge(CHALLENGE_VALUE)
                .build();
        AuditData auditData = challengeResponse.getAuditData();
        assertAll(
                () -> assertEquals(1, auditData.getAttributes().size()),
                () -> assertEquals(CHALLENGE_VALUE, auditData.getAttribute(ATTESTATION_CHALLENGE))
        );
    }

    @Test
    @DisplayName("When serializing with Jackson, then audit data is not exposed")
    public void testJacksonSerializationDoesNotExposeAuditData() throws Exception {
        ChallengeResponse challengeResponse = ChallengeResponse.builder()
                .attestationChallenge(CHALLENGE_VALUE)
                .build();
        String serializedJson = new ObjectMapper().writeValueAsString(challengeResponse);
        Map<String, Object> parsedJson = JsonUtils.parseJsonObject(serializedJson);

        assertAll(
                () -> assertEquals(CHALLENGE_VALUE, parsedJson.get(ATTESTATION_CHALLENGE)),
                () -> assertNull(parsedJson.get("auditData")),
                () -> assertEquals(1, parsedJson.size())
        );
    }
}
