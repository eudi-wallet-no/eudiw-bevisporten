package no.idporten.eudiw.issuer.claimssource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.config.ClaimsSourceProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ClaimsSourceService implements InitializingBean {

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;
    private final GenericApplicationContext applicationContext;
    private final List<ClaimsSource> claimsSources;

    public ClaimsSource findClaimsSource(String doctype) {
        return claimsSources.stream()
                .filter(claimsSource -> claimsSource.supports(doctype))
                .findFirst()
                .orElseThrow(() -> new IssuerServerException("server_error", "Unknown claims source for doctype [%s]".formatted(doctype), HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (ClaimsSourceProperties claimsSourceProperties : credentialIssuerServerProperties.getClaimsSources()) {
            ClaimsSource claimsSource = (ClaimsSource) applicationContext.getBean(Class.forName(claimsSourceProperties.getClassName()));
            claimsSource.init(claimsSourceProperties);
            log.info("Claims source initialized for doctype {}: {}", claimsSource.getProperties().getDoctype(), claimsSource.getClass().getName());
        }
        log.info("Claims source service managing {} claims sources", claimsSources.size());
    }

}
