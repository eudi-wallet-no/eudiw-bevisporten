package no.idporten.eudiw.issuer.claimssource.byob;

import no.idporten.eudiw.issuer.claimssource.AbstractPreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.CredentialData;
import no.idporten.eudiw.issuer.claimssource.PreAuthorizedIssuanceContext;
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
    public CredentialData push(PreAuthorizedIssuanceContext issuanceContext, CredentialData credentialData) {
        return credentialData;
    }

    @Override
    public String getAuthorativeSourceName() {
        return BYOB.name();
    }

}
