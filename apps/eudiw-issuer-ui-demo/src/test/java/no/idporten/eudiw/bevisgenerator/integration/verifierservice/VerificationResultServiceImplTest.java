package no.idporten.eudiw.bevisgenerator.integration.verifierservice;

import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerifiedCredential;
import no.idporten.eudiw.bevisgenerator.web.models.VerificationResultView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("When building verification result views")
class VerificationResultServiceImplTest {

    private static final String PID_TYPE = "eu.europa.ec.eudi.pid.1";
    private static final String AGE_TYPE = "proof_of_age";
    private static final String CREDENTIAL_NAME = "Personidentifikasjon";

    private final VerificationResultService verificationResultService = new VerificationResultServiceImpl();

    @Test
    @DisplayName("When a single credential is verified, then the display name is the requested credential name without a number")
    void singleCredentialUsesRequestedNameWithoutNumber() {
        Map<String, List<VerifiedCredential>> credentials = Map.of(PID_TYPE, List.of(credential()));

        List<VerificationResultView> views = verificationResultService.buildVerificationResultViews(credentials, CREDENTIAL_NAME);

        assertThat(views).extracting(VerificationResultView::displayName).containsExactly(CREDENTIAL_NAME);
    }

    @Test
    @DisplayName("When several credentials share one name, then the display names are numbered from newest to oldest")
    void credentialsWithTheSameNameAreNumbered() {
        Map<String, List<VerifiedCredential>> credentials = new LinkedHashMap<>();
        credentials.put(PID_TYPE, List.of(credential(), credential()));
        credentials.put(AGE_TYPE, List.of(credential()));

        List<VerificationResultView> views = verificationResultService.buildVerificationResultViews(credentials, CREDENTIAL_NAME);

        assertThat(views).extracting(VerificationResultView::credentialType, VerificationResultView::displayName)
                .containsExactly(
                        tuple(PID_TYPE, CREDENTIAL_NAME + " – bevis 1"),
                        tuple(PID_TYPE, CREDENTIAL_NAME + " – bevis 2"),
                        tuple(AGE_TYPE, CREDENTIAL_NAME + " – bevis 3")
                );
    }

    @Test
    @DisplayName("When the requested credential name is missing, then each credential type is used as display name without a number")
    void missingCredentialNameFallsBackToCredentialType() {
        Map<String, List<VerifiedCredential>> credentials = new LinkedHashMap<>();
        credentials.put(PID_TYPE, List.of(credential()));
        credentials.put(AGE_TYPE, List.of(credential()));

        List<VerificationResultView> views = verificationResultService.buildVerificationResultViews(credentials, null);

        assertThat(views).extracting(VerificationResultView::displayName)
                .containsExactly(PID_TYPE, "proof of age");
    }

    private VerifiedCredential credential() {
        return new VerifiedCredential(Map.of("given_name", "Kari"), true, List.of());
    }
}
