package no.idporten.eudiw.issuer.config;

import no.idporten.eudiw.issuer.IssuerServerException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
public class CredentialIssuerTenantService implements InitializingBean {

    public static final String ROOT_TENANT_ID = "root";

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;

    public CredentialIssuerTenantService(CredentialIssuerServerProperties credentialIssuerServerProperties) {
        this.credentialIssuerServerProperties = credentialIssuerServerProperties;
    }

    /**
     * Normalize tenant by replacing "" or null with the "root" tenant.
     */
    public String normalizeTenant(String tenant) {
        return StringUtils.hasLength(tenant)  ? tenant : ROOT_TENANT_ID;
    }

    public CredentialIssuerTenant findTenantById(String tenant) {
        CredentialIssuerTenant credentialIssuerTenant = credentialIssuerServerProperties.getTenants().get(normalizeTenant(tenant));
        if (credentialIssuerTenant == null) {
            throw new IssuerServerException("invalid_request", "Unknown credential issuer tenant.", HttpStatus.BAD_REQUEST);
        }
        return credentialIssuerTenant;
    }

    public List<CredentialIssuerTenant> findAllTenants() {
        return credentialIssuerServerProperties.getTenants().values().stream().toList();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (String tenantId : credentialIssuerServerProperties.getTenants().keySet()) {
            CredentialIssuerTenant tenant = credentialIssuerServerProperties.getTenants().get(tenantId);
            tenant.setId(tenantId);
            if (ROOT_TENANT_ID.equals(tenantId)) {
                tenant.setCredentialIssuer(credentialIssuerServerProperties.getCredentialIssuer());
            } else {
                tenant.setCredentialIssuer(UriComponentsBuilder.fromUri(credentialIssuerServerProperties.getCredentialIssuer()).pathSegment(tenantId).build().toUri());
            }
        }
    }

}
