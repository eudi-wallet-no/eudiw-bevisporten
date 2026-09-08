package no.idporten.eudiw.bevisgenerator.integration.verifierservice;

import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.ValidationDetail;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.ValidationStatus;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerifiedCredential;
import no.idporten.eudiw.bevisgenerator.web.models.ClaimView;
import no.idporten.eudiw.bevisgenerator.web.models.ValidationDetailView;
import no.idporten.eudiw.bevisgenerator.web.models.VerificationResultView;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class VerificationResultServiceImpl implements VerificationResultService {
    @Override
    public List<VerificationResultView> buildVerificationResultViews(Map<String, List<VerifiedCredential>> credentials) {
        if (credentials == null || credentials.isEmpty()) {
            return List.of();
        }

        return credentials.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(credential -> new VerificationResultView(
                                entry.getKey(),
                                formatCredentialType(entry.getKey()),
                                credential.isValid(),
                                ClaimView.from(credential.claims()),
                                buildValidationDetailViews(credential.validationDetails())
                        )))
                .toList();
    }

    private List<ValidationDetailView> buildValidationDetailViews(List<ValidationDetail> validationDetails) {
        if (validationDetails == null || validationDetails.isEmpty()) {
            return List.of();
        }

        return validationDetails.stream()
                .map(detail -> new ValidationDetailView(
                        detail.validationDetails(),
                        validationStatusColor(detail.status())
                ))
                .toList();
    }

    private String formatCredentialType(String credentialType) {
        return credentialType.replace('_', ' ');
    }

    private String validationStatusColor(ValidationStatus status) {
        return switch (status) {
            case VALID -> "success";
            case INVALID -> "danger";
            case INCONCLUSIVE -> "warning";
            case NOT_APPLICABLE -> "info";
        };
    }
}
