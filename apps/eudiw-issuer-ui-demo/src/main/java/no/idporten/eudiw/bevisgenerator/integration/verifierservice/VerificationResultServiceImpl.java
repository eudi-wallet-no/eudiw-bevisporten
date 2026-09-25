package no.idporten.eudiw.bevisgenerator.integration.verifierservice;

import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.ValidationDetail;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.ValidationStatus;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerifiedCredential;
import no.idporten.eudiw.bevisgenerator.web.models.ClaimView;
import no.idporten.eudiw.bevisgenerator.web.models.ValidationDetailView;
import no.idporten.eudiw.bevisgenerator.web.models.VerificationResultView;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VerificationResultServiceImpl implements VerificationResultService {
    @Override
    public List<VerificationResultView> buildVerificationResultViews(Map<String, List<VerifiedCredential>> credentials, String requestedCredentialName) {
        if (credentials == null || credentials.isEmpty()) {
            return List.of();
        }

        List<Map.Entry<String, VerifiedCredential>> orderedCredentials = credentials.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(credential -> Map.entry(entry.getKey(), credential)))
                .toList();

        List<String> displayNames = buildDisplayNames(orderedCredentials, requestedCredentialName);

        List<VerificationResultView> views = new ArrayList<>();
        for (int i = 0; i < orderedCredentials.size(); i++) {
            Map.Entry<String, VerifiedCredential> entry = orderedCredentials.get(i);
            views.add(new VerificationResultView(
                    entry.getKey(),
                    displayNames.get(i),
                    entry.getValue().isValid(),
                    ClaimView.from(entry.getValue().claims()),
                    buildValidationDetailViews(entry.getValue().validationDetails())
            ));
        }
        return views;
    }

    private String baseName(String credentialType, String requestedCredentialName) {
        return StringUtils.hasText(requestedCredentialName)
                ? requestedCredentialName
                : formatCredentialType(credentialType);
    }

    /**
     * The wallet presents the newest credential first, so the running number reflects age:
     * 1 is the newest credential with that name. A name that appears only once is shown without a number.
     */
    private List<String> buildDisplayNames(List<Map.Entry<String, VerifiedCredential>> orderedCredentials, String requestedCredentialName) {
        List<String> baseNames = orderedCredentials.stream()
                .map(entry -> baseName(entry.getKey(), requestedCredentialName))
                .toList();

        Map<String, Long> countPerName = baseNames.stream()
                .collect(Collectors.groupingBy(name -> name, Collectors.counting()));
        Map<String, Integer> numberPerName = new HashMap<>();

        List<String> displayNames = new ArrayList<>();
        for (String name : baseNames) {
            if (countPerName.get(name) > 1) {
                int number = numberPerName.merge(name, 1, Integer::sum);
                displayNames.add("%s – bevis %d".formatted(name, number));
            } else {
                displayNames.add(name);
            }
        }
        return displayNames;
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
