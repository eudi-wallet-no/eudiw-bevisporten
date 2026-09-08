package no.idporten.eudiw.bevisgenerator.integration.verifierservice;

import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerifiedCredential;
import no.idporten.eudiw.bevisgenerator.web.models.VerificationResultView;

import java.util.List;
import java.util.Map;

public interface VerificationResultService {
    List<VerificationResultView> buildVerificationResultViews(Map<String, List<VerifiedCredential>> credentials);
}
