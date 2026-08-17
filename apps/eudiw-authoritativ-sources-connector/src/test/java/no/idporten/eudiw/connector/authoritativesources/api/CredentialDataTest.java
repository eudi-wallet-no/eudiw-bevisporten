package no.idporten.eudiw.connector.authoritativesources.api;

import no.idporten.eudiw.connector.authoritativesources.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("When creating CredentialData using .of(..)")
public class CredentialDataTest {
    @Test
    @DisplayName("then is should succeed when adding lists and maps to CredentialData")
    public void ofShouldSucceedWhenAddingObject() {
        CredentialData credentialData = new CredentialData();
        Subject subject = TestData.getValidSubject();
        Map<String, String> subjectMap = Map.of("identifier", subject.identifier());
        credentialData.addStringMap("subject", subjectMap);

        List<Number> list = Arrays.asList(1, 2, 3);
        credentialData.addNumberList("list", list);

        assertTrue(credentialData.containsKey("list"));
        assertTrue(credentialData.containsKey("subject"));
        assertEquals(list, credentialData.get("list"));
        assertEquals(subjectMap, credentialData.get("subject"));
    }
}
