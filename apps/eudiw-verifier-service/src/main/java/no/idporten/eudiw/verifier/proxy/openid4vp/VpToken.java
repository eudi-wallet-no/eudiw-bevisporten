package no.idporten.eudiw.verifier.proxy.openid4vp;

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
}
