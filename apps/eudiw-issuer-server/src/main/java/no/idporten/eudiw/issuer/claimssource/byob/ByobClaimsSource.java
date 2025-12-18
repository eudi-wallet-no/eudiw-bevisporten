package no.idporten.eudiw.issuer.claimssource.byob;

import no.idporten.eudiw.issuer.claimssource.AbstractPreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.PreAuthorizedIssuanceContext;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ByobClaimsSource extends AbstractPreAuthorizedClaimsSource {

    private final ByobCredentialConfigurationService credentialConfigurationService;

    public ByobClaimsSource(ByobCredentialConfigurationService credentialConfigurationService) {
        this.credentialConfigurationService = credentialConfigurationService;
    }

    @Override
    public boolean supports(String credentialType) {
        return credentialConfigurationService.getDocumentMetadata(credentialType) != null;
    }

    @Override
    public DocumentMetadata getDocumentMetadata() {
        String credentialConfigurationId = "eidas2sandkasse.wildcard_sd_jwt_vc"; // TODO egentlig credential type
        return credentialConfigurationService.getDocumentMetadata(credentialConfigurationId);
    }

    @Override
    public Map<String, Object> push(PreAuthorizedIssuanceContext issuanceContext, Map<String, String> claims) {
        return new HashMap<>(claims);
    }

}
