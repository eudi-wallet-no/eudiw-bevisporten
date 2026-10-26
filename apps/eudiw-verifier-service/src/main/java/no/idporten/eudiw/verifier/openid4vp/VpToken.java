package no.idporten.eudiw.verifier.openid4vp;

import no.idporten.eudiw.verifier.VerificationException;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record VpToken(
        Map<String, List<VerifiablePresentation>> verifiablePresentations
) {

    public List<VerifiablePresentation> getVerifiablePresentation(String credentialId) {
        List<VerifiablePresentation> presentations = verifiablePresentations.get(credentialId);
        if (presentations == null) {
            return List.of();
        }
        return presentations;
    }

    public String toJson() {
        Map<String, List<String>> jsonMap = new HashMap<>();
        for (var entry : verifiablePresentations.entrySet()) {
            jsonMap.put(entry.getKey(),
                    entry.getValue().stream()
                            .map(VerifiablePresentation::value)
                            .toList());
        }
        try {
            return new ObjectMapper().writeValueAsString(jsonMap);
        } catch (Exception e) {
            throw new VerificationException("error", "Failed to convert vpToken to json for logging", e);
        }

    }
}
