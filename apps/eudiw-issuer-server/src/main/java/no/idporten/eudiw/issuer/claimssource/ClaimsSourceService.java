package no.idporten.eudiw.issuer.claimssource;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.claimssource.pid.PIDClaimsSource;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClaimsSourceService implements InitializingBean {

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;
    private List<ClaimsSource> claimsSources;

    public ClaimsSource findClaimsSource(String doctype) {
        return claimsSources.stream()
                .filter(claimsSource -> claimsSource.supports(doctype))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.claimsSources = this.credentialIssuerServerProperties.getClaimsSources().stream()
                .map(claimsSourceProperties -> {
                    ClaimsSource claimsSource  = new PIDClaimsSource();
                    claimsSource.init(claimsSourceProperties);
                    return claimsSource;
                })
                .toList();
    }

}
