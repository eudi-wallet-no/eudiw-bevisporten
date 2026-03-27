package no.idporten.eudiw.issuer.claimssource;

import lombok.SneakyThrows;
import no.idporten.eudiw.issuer.authoritativesources.AuthoritativeSourceService;
import no.idporten.eudiw.issuer.credentials.types.Claim;
import org.springframework.stereotype.Service;

import java.util.List;

import static no.idporten.eudiw.issuer.authoritativesources.AuthoritativeSource.BYOB;

/**
 * Generic claims source for issuance where the credential data is pulled from a service implementing the
 * authoritative source API.
 */
@Service
public class AuthoritativeSourcePullClaimsSource extends AbstractPreAuthorizedClaimsSource implements AuthorizedClaimsSource {

    private final AuthoritativeSourceService authoritativeSourceService;

    public AuthoritativeSourcePullClaimsSource(AuthoritativeSourceService authoritativeSourceService) {
        this.authoritativeSourceService = authoritativeSourceService;
    }

    public List<Claim> issueClaims(CredentialIssueContext credentialIssueContext) {
        if (credentialIssueContext.transactionId() != null) {
            return super.issueClaims(credentialIssueContext);
        }
        CredentialData credentialData = pull(credentialIssueContext);
        credentialData = validate(credentialIssueContext.credentialConfiguration().getExtendedCredentialMetadata(), credentialData);
        return convertCredentialDataToClaims(credentialIssueContext, credentialData.claims());
    }

    @Override
    public CredentialData pull(PreAuthorizedIssuanceContext issuanceContext) {
        final String personIdentifier = issuanceContext.personIdentifier();
        final String credentialType = issuanceContext.credentialConfiguration().getCredentialType();
        final String source = issuanceContext.credentialConfiguration().getCredentialIssuerContext().getCredentialDataSourceUri().getAuthority();
        return authoritativeSourceService.retrieveCredentialData(source, credentialType, personIdentifier);
    }

    @SneakyThrows
    @Override
    public CredentialData pull(CredentialIssueContext context) {
        final String personIdentifier = context.personIdentifier();
        final String credentialType = context.credentialConfiguration().getCredentialType();
        final String source = context.credentialConfiguration().getCredentialIssuerContext().getCredentialDataSourceUri().getAuthority();
        return authoritativeSourceService.retrieveCredentialData(source, credentialType, personIdentifier);
    }

    @Override
    public String getAuthorativeSourceName() {
        // TODO fjerne dette fra utsteder og exceptions, gir ikke så stor mening når alle er flyttet til ny connector - egen sak!
        return BYOB.name();
    }

}
