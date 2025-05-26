package no.idporten.eudiw.issuer.claimssource;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.config.ClaimsSourceProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;

@RequiredArgsConstructor
@Configuration
public class ClaimsSourceConfiguration implements InitializingBean {

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;
    private final GenericApplicationContext applicationContext;

    @Override
    public void afterPropertiesSet() throws Exception {
        for (ClaimsSourceProperties claimsSourceProperties : credentialIssuerServerProperties.getClaimsSources()) {
            applicationContext.registerBean(claimsSourceProperties.getDoctype() + "-ClaimsSource", ClaimsSource.class, () -> {
                try {
                    Class<?> clazz = Class.forName(claimsSourceProperties.getClassName());
                    ClaimsSource claimsSource = (ClaimsSource) clazz.getConstructor().newInstance();
                    claimsSource.init(claimsSourceProperties);
                    return claimsSource;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
