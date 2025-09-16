package no.idporten.eudiw.issuer.openid4vci.service;

public record IssuanceStatus(
        IssuanceTransactionId issuanceTransactionId,
        String credentialConfigurationId,
        String status) {
}
