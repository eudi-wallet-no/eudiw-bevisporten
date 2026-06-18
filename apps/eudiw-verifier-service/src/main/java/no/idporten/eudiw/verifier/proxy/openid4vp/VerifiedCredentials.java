package no.idporten.eudiw.verifier.proxy.openid4vp;

import java.util.List;
import java.util.Map;

public record VerifiedCredentials (
        Map<String, List<VerifiedCredential>> credentials

) {
}
