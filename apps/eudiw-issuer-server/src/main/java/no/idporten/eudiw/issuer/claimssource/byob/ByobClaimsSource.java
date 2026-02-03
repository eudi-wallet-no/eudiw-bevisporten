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

    public static final String DYNAMIC_CREDENTIAL_CONFIGURATION_PREFIX = "net.eidas2sandkasse:";

    private final DynamicCredentialConfigurationService credentialConfigurationService;

    public ByobClaimsSource(DynamicCredentialConfigurationService credentialConfigurationService) {
        this.credentialConfigurationService = credentialConfigurationService;
    }

    @Override
    public boolean supports(String credentialType) {
        if (!credentialType.startsWith(DYNAMIC_CREDENTIAL_CONFIGURATION_PREFIX)) {
            return false;
        }

        try {
            return credentialConfigurationService.getDocumentMetadataByCredentialType(credentialType) != null;
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
        if (credentialMetadataContext.credentialType() != null && credentialMetadataContext.credentialType().startsWith(DYNAMIC_CREDENTIAL_CONFIGURATION_PREFIX)) {
            return credentialConfigurationService.getDocumentMetadataByCredentialType(credentialMetadataContext.credentialType());
        }
        if (credentialMetadataContext.credentialConfigurationId() != null && credentialMetadataContext.credentialConfigurationId().startsWith(DYNAMIC_CREDENTIAL_CONFIGURATION_PREFIX)) {
            return credentialConfigurationService.getDocumentMetadata(credentialMetadataContext.credentialConfigurationId());
        }
        log.warn("credentialType ({}) or credentialConfigurationId ({}) does not start with {} for BYOB claimssource, returning null metadata", credentialMetadataContext.credentialType(), credentialMetadataContext.credentialConfigurationId(), DYNAMIC_CREDENTIAL_CONFIGURATION_PREFIX);
        return null;
        //throw new ClaimsSourceInvalidDataException(getAuthorativeSourceName(), "credentialType (%s) or credentialConfigurationId (%s) must start with %s for BYOB claimssource".formatted(credentialMetadataContext.credentialType(), credentialMetadataContext.credentialConfigurationId(), DYNAMIC_CREDENTIAL_CONFIGURATION_PREFIX));
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
