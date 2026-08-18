package no.idporten.eudiw.issuer.issuance.authz;

import lombok.Getter;

@Getter
public class SubjectCredentialIssuanceTransactionEntity {
    private final Long subjectCredentialTransactionId;
    private final String subjectIdentifier;
    private final String issuanceTransactionId;
    private final long subjectCredentialTransactionCreatedMs;
    private final Long credentialIssuanceTransactionId;
    private final String credentialConfigurationId;
    private final String credentialIssuerTenant;
    private final long credentialIssuanceTransactionCreatedMs;
    private final long credentialIssuanceTransactionUpdatedMs;
    private final String credentialIssuanceTransactionStatus;
    private final String credentialIssuanceTransactionNotificationId;

    public SubjectCredentialIssuanceTransactionEntity(
            Long subjectCredentialTransactionId,
            String subjectIdentifier,
            String issuanceTransactionId,
            long subjectCredentialTransactionCreatedMs,
            Long credentialIssuanceTransactionId,
            String credentialConfigurationId,
            String credentialIssuerTenant,
            long credentialIssuanceTransactionCreatedMs,
            long credentialIssuanceTransactionUpdatedMs,
            String credentialIssuanceTransactionStatus,
            String credentialIssuanceTransactionNotificationId
    ) {
        this.subjectCredentialTransactionId = subjectCredentialTransactionId;
        this.subjectIdentifier = subjectIdentifier;
        this.issuanceTransactionId = issuanceTransactionId;
        this.subjectCredentialTransactionCreatedMs = subjectCredentialTransactionCreatedMs;
        this.credentialIssuanceTransactionId = credentialIssuanceTransactionId;
        this.credentialConfigurationId = credentialConfigurationId;
        this.credentialIssuerTenant = credentialIssuerTenant;
        this.credentialIssuanceTransactionCreatedMs = credentialIssuanceTransactionCreatedMs;
        this.credentialIssuanceTransactionUpdatedMs = credentialIssuanceTransactionUpdatedMs;
        this.credentialIssuanceTransactionStatus = credentialIssuanceTransactionStatus;
        this.credentialIssuanceTransactionNotificationId = credentialIssuanceTransactionNotificationId;
    }

}
