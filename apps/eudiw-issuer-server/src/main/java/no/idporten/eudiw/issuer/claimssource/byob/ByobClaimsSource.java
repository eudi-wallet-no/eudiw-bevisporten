package no.idporten.eudiw.issuer.claimssource.byob;

import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.AbstractPreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.CredentialData;
import no.idporten.eudiw.issuer.claimssource.CredentialMetadataContext;
import no.idporten.eudiw.issuer.claimssource.PreAuthorizedIssuanceContext;
import no.idporten.eudiw.issuer.credentials.types.DocumentMetadata;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceInvalidDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static no.idporten.eudiw.issuer.authoritativesources.AuthoritativeSource.BYOB;

@Service
public class ByobClaimsSource extends AbstractPreAuthorizedClaimsSource {

    private static final Logger log = LoggerFactory.getLogger(ByobClaimsSource.class);

    private final DynamicCredentialConfigurationService dynamicCredentialConfigurationService;

    public ByobClaimsSource(DynamicCredentialConfigurationService dynamicCredentialConfigurationService) {
        this.dynamicCredentialConfigurationService = dynamicCredentialConfigurationService;
    }

    @Override
    public boolean supports(String credentialType) {
        try {
            return dynamicCredentialConfigurationService.getDocumentMetadataByCredentialType(credentialType) != null;
        } catch (IssuerServerException e) {
            log.error("Error checking support for credentialType={}. Ignore and continue", credentialType, e);
            return false;
        }
    }

    @Override
    public DocumentMetadata getDocumentMetadata(CredentialMetadataContext credentialMetadataContext) {
        if (credentialMetadataContext == null) {
            throw new ClaimsSourceInvalidDataException(getAuthorativeSourceName(), "credentialMetadataContext cannot be null for BYOB claimssource");
        }
        if (credentialMetadataContext.credentialType() != null) {
            return dynamicCredentialConfigurationService.getDocumentMetadataByCredentialType(credentialMetadataContext.credentialType());
        }
        if (credentialMetadataContext.credentialConfigurationId() != null) {
            return dynamicCredentialConfigurationService.getDocumentMetadata(credentialMetadataContext.credentialConfigurationId());
        }
        log.warn("credentialType ({}) or credentialConfigurationId ({}) not found for BYOB claimssource, returning null metadata", credentialMetadataContext.credentialType(), credentialMetadataContext.credentialConfigurationId());
        return null;
    }

    @Override
    public CredentialData push(PreAuthorizedIssuanceContext issuanceContext, CredentialData credentialData) {
        return credentialData;
    }

    @Override
    public String getAuthorativeSourceName() {
        return BYOB.name();
    }

}
