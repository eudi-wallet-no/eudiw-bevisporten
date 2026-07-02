package no.idporten.eudiw.issuer.issuance.authz;

import lombok.Getter;

@Getter
public class SubjectCredentialTransactionEntity {

    private final Long id;
    private final String subjectIdentifier;
    private final String issuanceTransactionId;
    private final long createdMs;

    public SubjectCredentialTransactionEntity(
            Long id,
            String subjectIdentifier,
            String issuanceTransactionId,
            long createdMs
    ) {
        this.id = id;
        this.subjectIdentifier = subjectIdentifier;
        this.issuanceTransactionId = issuanceTransactionId;
        this.createdMs = createdMs;
    }

}
