package no.idporten.eudiw.issuer.claimssource.byob;

import no.idporten.eudiw.issuer.claimssource.AbstractPreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.CredentialData;
import no.idporten.eudiw.issuer.claimssource.CredentialMetadataContext;
import no.idporten.eudiw.issuer.claimssource.PreAuthorizedIssuanceContext;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceInvalidDataException;
import org.springframework.stereotype.Service;

import static no.idporten.eudiw.issuer.claimssource.AuthoritativeSource.BYOB;

@Service
public class ByobClaimsSource extends AbstractPreAuthorizedClaimsSource {

    private final DynamicCredentialConfigurationService credentialConfigurationService;

    public ByobClaimsSource(DynamicCredentialConfigurationService credentialConfigurationService) {
        this.credentialConfigurationService = credentialConfigurationService;
    }

    @Override
    public boolean supports(String credentialType) {
        return credentialConfigurationService.getDocumentMetadataByCredentialType(credentialType) != null;
    }

    @Override
    public DocumentMetadata getDocumentMetadata(CredentialMetadataContext credentialMetadataContext) {
        if (credentialMetadataContext == null) {
           throw new ClaimsSourceInvalidDataException(getAuthorativeSourceName(), "credentialMetadataContext cannot be null for byob claims source");
        }
        if (credentialMetadataContext.credentialConfigurationId() != null) {
            return credentialConfigurationService.getDocumentMetadata(credentialMetadataContext.credentialConfigurationId());
        }
        return credentialConfigurationService.getDocumentMetadataByCredentialType(credentialMetadataContext.credentialType());
    }

    @Override
    public CredentialData push(PreAuthorizedIssuanceContext issuanceContext, CredentialData credentialData) {
        return credentialData;
    }

    @Override
    public String getAuthorativeSourceName(){
        return BYOB.name();
    }

}
