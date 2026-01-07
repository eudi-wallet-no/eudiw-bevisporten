package no.idporten.eudiw.issuer.claimssource;

import java.util.Map;

public record CredentialData(Map<String, Object> claims, String credentialConfigurationId) {
}
