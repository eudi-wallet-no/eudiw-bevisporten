package no.idporten.eudiw.issuer.claimssource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.issuer.IssuerServerException;
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

    public ClaimsSource findClaimsSource(String credentialType) {
        return claimsSources.stream()
                .filter(claimsSource -> claimsSource.supports(credentialType))
                .findFirst()
                .orElseThrow(() -> new IssuerServerException("server_error", "Unknown claims source for credential type [%s]".formatted(credentialType), HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (ClaimsSourceProperties claimsSourceProperties : credentialIssuerServerProperties.getClaimsSources()) {
            ClaimsSource claimsSource = (ClaimsSource) applicationContext.getBean(Class.forName(claimsSourceProperties.getClassName()));
            claimsSource.init(claimsSourceProperties);
            log.info("Claims source initialized for credential types {}: {}", claimsSource.getProperties().getCredentialTypes(), claimsSource.getClass().getName());
        }
        log.info("Claims source service managing {} claims sources", claimsSources.size());
    }

}
