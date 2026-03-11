package no.idporten.eudiw.connector.authoritativsources.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When creating CredentialData using .of(..)")
public class CredentialDataTest {

    @Test
    @DisplayName("then it should fail with odd number of input variables")
    public void ofShouldFailIfOddNumber() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            CredentialData.of("Key1", "Value1", "Key2");
        });

        assertTrue(exception.getMessage().contains("Arguments must be key-value pairs"));
    }

    @Test
    @DisplayName("then it should succeed with even number of input variables")
    public void ofShouldSucceedIfEvenNumber() {
        CredentialData credentialData = CredentialData.of("Key1", "Value1", "Key2", "Value2");

        assertTrue(credentialData.containsKey("Key1"));
        assertTrue(credentialData.containsKey("Key2"));
        assertEquals("Value1", credentialData.get("Key1"));
        assertEquals("Value2", credentialData.get("Key2"));
   }

    @Test
    @DisplayName("then it should succeed with no of input variables")
    public void ofShouldSucceedIfEmpty() {
        CredentialData credentialData = CredentialData.of();
        assertTrue(credentialData.isEmpty());
    }
}
