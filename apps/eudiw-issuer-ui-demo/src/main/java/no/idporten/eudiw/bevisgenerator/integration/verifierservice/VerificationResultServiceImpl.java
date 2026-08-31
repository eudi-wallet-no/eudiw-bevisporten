package no.idporten.eudiw.bevisgenerator.integration.verifierservice;

import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.ValidationDetail;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.ValidationStatus;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.ValidationType;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.VerifiedCredential;
import no.idporten.eudiw.bevisgenerator.web.models.ClaimView;
import no.idporten.eudiw.bevisgenerator.web.models.ValidationDetailView;
import no.idporten.eudiw.bevisgenerator.web.models.VerificationResultView;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                                buildClaimViews(credential.claims()),
                                buildValidationDetailViews(credential.validationDetails())
                        )))
                .toList();
    }

    private List<ClaimView> buildClaimViews(Map<String, Object> claims) {
        if (claims == null || claims.isEmpty()) {
            return List.of();
        }

        return unwrapNamespaceClaims(claims).entrySet().stream()
                .map(entry -> buildClaimView(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Map<String, Object> unwrapNamespaceClaims(Map<String, Object> claims) {
        while (claims.size() == 1) {
            Object onlyValue = claims.values().iterator().next();
            if (!(onlyValue instanceof Map<?, ?> nested)) {
                break;
            }
            claims = asStringKeyedMap(nested);
        }
        return claims;
    }

    private ClaimView buildClaimView(String name, Object value) {
        String label = formatClaimName(name);

        if (value instanceof Map<?, ?> map) {
            return new ClaimView(label, null, buildClaimViews(asStringKeyedMap(map)));
        }

        if (value instanceof List<?> list && containsNestedValues(list)) {
            List<ClaimView> children = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                children.add(buildClaimView(name + " " + (i + 1), list.get(i)));
            }
            return new ClaimView(label, null, children);
        }

        return new ClaimView(label, formatClaimValue(value), List.of());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asStringKeyedMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    private boolean containsNestedValues(List<?> list) {
        return list.stream().anyMatch(item -> item instanceof Map<?, ?> || item instanceof List<?>);
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

    private String formatClaimName(String name) {
        return name.replace('_', ' ');
    }

    private String formatClaimValue(Object value) {
        return switch (value) {
            case null -> "–";
            case Boolean bool -> bool ? "Ja" : "Nei";
            case List<?> list -> list.stream().map(this::formatClaimValue).collect(Collectors.joining(", "));
            default -> value.toString();
        };
    }

    private String validationStatusColor(ValidationStatus status) {
        return switch (status) {
            case VALID -> "success";
            case INVALID -> "danger";
            case INCONCLUSIVE -> "warning";
        };
    }
}
