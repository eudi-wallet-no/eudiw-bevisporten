package no.idporten.eudiw.issuer.openid4vci.service;

public record IssuanceStatus(
        IssuerTransactionId issuerTransactionId,
        String credentialConfigurationId,
        String status) {
}
