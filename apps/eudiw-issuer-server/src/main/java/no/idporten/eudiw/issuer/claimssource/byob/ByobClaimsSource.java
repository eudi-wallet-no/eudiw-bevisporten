package no.idporten.eudiw.issuer.claimssource.byob;

import no.idporten.eudiw.issuer.claimssource.AbstractPreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.CredentialMetadataContext;
import no.idporten.eudiw.issuer.claimssource.PreAuthorizedIssuanceContext;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

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
        // TODO finn på ulike måter eller skjerp context litt?
        if (credentialMetadataContext == null) {
            return credentialConfigurationService.getDocumentMetadataByCredentialType("dynamic:1");
        }
        if (credentialMetadataContext.credentialConfigurationId() != null) {
            return credentialConfigurationService.getDocumentMetadata(credentialMetadataContext.credentialConfigurationId());
        }
        return credentialConfigurationService.getDocumentMetadataByCredentialType(credentialMetadataContext.credentialType());
    }

    @Override
    public Map<String, Object> push(PreAuthorizedIssuanceContext issuanceContext, Map<String, String> claims) {
        return new HashMap<>(claims);
    }

}
