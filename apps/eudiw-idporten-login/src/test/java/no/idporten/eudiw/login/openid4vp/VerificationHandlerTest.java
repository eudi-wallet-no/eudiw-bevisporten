package no.idporten.eudiw.login.openid4vp;

import no.idporten.eudiw.login.AcrValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("When creating verification handlers")
public class VerificationHandlerTest {

    @DisplayName("then create an EU verification handler for eidas acr values")
    @ParameterizedTest
    @EnumSource(value = AcrValue.class, names = {"EIDAS_LOA_SUBSTANTIAL", "EIDAS_LOA_HIGH"})
    void testCreateEUVerificationHandler(AcrValue acrValue) {
        assertTrue(VerificationHandler.forAcrValue(acrValue) instanceof EUVerificationHandler);
    }

    @DisplayName("then create a NO verification handler for idporten acr values")
    @ParameterizedTest
    @EnumSource(value = AcrValue.class, names = {"IDPORTEN_LOA_SUBSTANTIAL", "IDPORTEN_LOA_HIGH"})
    void testCreateNOVerificationHandler(AcrValue acrValue) {
        assertTrue(VerificationHandler.forAcrValue(acrValue) instanceof NOVerificationHandler);
    }

}
