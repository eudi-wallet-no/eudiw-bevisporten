package no.idporten.eudiw.verifier.openid4vp;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record VpToken(
        Map<String, List<VerifiablePresentation>> verifiablePresentations
) {

    public String auditLogFormatted() {
        return verifiablePresentations.entrySet().stream()
                .map(entry -> "%s:[%s]".formatted(
                        jsonString(entry.getKey()),
                        entry.getValue().stream()
                                .map(presentation -> jsonString(presentation.value()))
                                .collect(Collectors.joining(","))))
                .collect(Collectors.joining(",", "{", "}"));
    }

    public List<VerifiablePresentation> getVerifiablePresentation(String credentialId) {
        List<VerifiablePresentation> presentations = verifiablePresentations.get(credentialId);
        if (presentations == null) {
            return List.of();
        }
        return presentations;
    }

    private static String jsonString(String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }
}
