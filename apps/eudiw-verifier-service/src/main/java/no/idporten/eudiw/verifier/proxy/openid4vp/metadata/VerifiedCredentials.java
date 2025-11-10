package no.idporten.eudiw.verifier.proxy.openid4vp.metadata;

import java.util.Map;

public record VerifiedCredentials (
        String vpToken,
        Map<String, Object> credentials

) {
}
