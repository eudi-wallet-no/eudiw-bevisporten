package no.idporten.eudiw.bevisgenerator.integration.verifierservice.model;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class VerifiedCredentialContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesWhenValidFieldIsMissing() {
        String json = """
                {
                  "claims": {"age_over_18": true}
                }
                """;

        VerifiedCredential credential = objectMapper.readValue(json, VerifiedCredential.class);

        assertThat(credential.isValid()).isFalse();
        assertThat(credential.claims()).containsEntry("age_over_18", true);
    }

    @Test
    void deserializesWhenValidFieldIsTrue() {
        String json = """
                {
                  "claims": {"age_over_18": true},
                  "valid": true
                }
                """;

        VerifiedCredential credential = objectMapper.readValue(json, VerifiedCredential.class);

        assertThat(credential.isValid()).isTrue();
    }
}
