package no.idporten.eudiw.bevisgenerator.web.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A single claim, formatted as display-ready text. If the underlying claim value was
 * a nested object (or a list of objects), {@code children} holds the nested claims and
 * {@code value} is null; the template renders children recursively as a nested list.
 */
public record ClaimView(String rawName, String name, String value, List<ClaimView> children) implements Serializable {

    public boolean isGroup() {
        return children != null && !children.isEmpty();
    }

    public static List<ClaimView> from(Map<String, Object> claims) {
        if (claims == null || claims.isEmpty()) {
            return List.of();
        }

        return unwrapNamespaceClaims(claims).entrySet().stream()
                .map(entry -> buildClaimView(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static Map<String, Object> unwrapNamespaceClaims(Map<String, Object> claims) {
        while (claims.size() == 1) {
            Object onlyValue = claims.values().iterator().next();
            if (!(onlyValue instanceof Map<?, ?> nested)) {
                break;
            }
            claims = asStringKeyedMap(nested);
        }
        return claims;
    }

    private static ClaimView buildClaimView(String name, Object value) {
        String label = formatClaimName(name);

        if (value instanceof Map<?, ?> map) {
            return new ClaimView(name, label, null, from(asStringKeyedMap(map)));
        }

        if (value instanceof List<?> list && containsNestedValues(list)) {
            List<ClaimView> children = new ArrayList<>();
            for (int i = 0; i < list.size(); i++) {
                children.add(buildClaimView(name + " " + (i + 1), list.get(i)));
            }
            return new ClaimView(name, label, null, children);
        }

        return new ClaimView(name, label, formatClaimValue(value), List.of());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asStringKeyedMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    private static boolean containsNestedValues(List<?> list) {
        return list.stream().anyMatch(item -> item instanceof Map<?, ?> || item instanceof List<?>);
    }

    private static String formatClaimName(String name) {
        return name.replace('_', ' ');
    }

    private static String formatClaimValue(Object value) {
        return switch (value) {
            case null -> "–";
            case Boolean bool -> bool ? "Ja" : "Nei";
            case List<?> list -> list.stream().map(ClaimView::formatClaimValue).collect(Collectors.joining(", "));
            default -> value.toString();
        };
    }
}
