package no.idporten.eudiw.issuer.issuance.authz;

import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.ErrorCode;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenantService;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.logging.audit.AuditService;
import no.idporten.eudiw.issuer.openid4vci.protocol.AuthorizedCodeGrant;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialOffer;
import no.idporten.eudiw.issuer.openid4vci.protocol.Grants;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


@Service
public class CredentialOfferService {

    public static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";

    private final CredentialIssuerTenantService credentialIssuerTenantService;
    private final AuditService auditService;

    public CredentialOfferService(CredentialIssuerTenantService credentialIssuerTenantService, AuditService auditService) {
        this.credentialIssuerTenantService = credentialIssuerTenantService;
        this.auditService = auditService;
    }

    public CredentialOffer createCredentialOffer(String tenant, String credentialConfigurationId) {
        return createCredentialOffer(credentialIssuerTenantService.findTenantById(tenant), Collections.singleton(credentialConfigurationId));
    }

    public CredentialOffer createCredentialOffer(String tenant, Set<String> credentialConfigurationIds) {
        return createCredentialOffer(credentialIssuerTenantService.findTenantById(tenant), credentialConfigurationIds);
    }

    public CredentialOffer createCredentialOffer(CredentialIssuerTenant tenant, Set<String> credentialConfigurationIds) {
        List<String> validCredentialConfigurationIds = new ArrayList<>();
        for (String credentialConfigurationId : credentialConfigurationIds) {
            ExtendedCredentialConfiguration credentialConfiguration = tenant.findCredentialConfiguration(credentialConfigurationId);
            if (!GRANT_TYPE_AUTHORIZATION_CODE.equals(credentialConfiguration.getCredentialIssuerContext().getGrantType())) {
                throw new IssuerServerException(ErrorCode.INVALID_REQUEST, "Credential configuration cannot be used with the authorization code flow");
            }
            validCredentialConfigurationIds.add(credentialConfiguration.getCredentialConfigurationId());
        }
        auditService.logCreateCredentialOffer(tenant.getCredentialIssuer().toString(), validCredentialConfigurationIds);
        return CredentialOffer.builder()
                .credentialIssuer(tenant.getCredentialIssuer().toString())
                .credentialConfigurationIds(validCredentialConfigurationIds)
                .grants(Grants.builder().authorizedCodeGrant(AuthorizedCodeGrant.builder().build()).build())
                .build();
    }

}
