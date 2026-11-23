package no.idporten.eudiw.oauth2.server;

import net.eidas2sandkasse.tsl.TokenStatusListValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TokenStatusListValidatorLibTest {

    private net.eidas2sandkasse.tsl.TokenStatusListValidator validator;

    public TokenStatusListValidatorLibTest() {
        this.validator = new TokenStatusListValidator();
    }

    @Test
    public void testTokenStatusListValidatorLibImportsToProject() {
        Assertions.assertNotNull(validator);
    }
}
