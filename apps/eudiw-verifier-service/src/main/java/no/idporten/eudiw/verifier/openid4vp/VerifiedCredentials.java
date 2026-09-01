package no.idporten.eudiw.verifier.openid4vp;

import tools.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@JsonSerialize
public record VerifiedCredentials (
        Map<String, List<VerifiedCredential>> credentials
) implements Serializable {
}
