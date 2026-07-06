package no.idporten.eudiw.bevisgenerator.integration.issuerserver.model;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RevokeBySubjectRequestContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesWithIssuerServerFieldNames() {
        RevokeBySubjectRequest revokeBySubjectRequest = new RevokeBySubjectRequest(
                "no.digdir.eudiw.pid_mso_mdoc",
                "05821098825"
        );

        String jsonPayload = objectMapper.writeValueAsString(revokeBySubjectRequest);
        JsonNode jsonNode = objectMapper.readTree(jsonPayload);

        assertTrue(jsonNode.has("credential_configuration_id"));
        assertTrue(jsonNode.has("subject_identifier"));
        assertEquals("no.digdir.eudiw.pid_mso_mdoc", jsonNode.get("credential_configuration_id").asText());
        assertEquals("05821098825", jsonNode.get("subject_identifier").asText());
    }
}
