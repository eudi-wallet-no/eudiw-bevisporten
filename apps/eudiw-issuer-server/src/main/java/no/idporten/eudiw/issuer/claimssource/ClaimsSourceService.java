package no.idporten.eudiw.issuer.claimssource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.openid4vci.metadata.ClaimsDescription;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;
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
        log.info("Finding claims source for credential type {}", credentialType);
        return claimsSources.stream()
                .filter(claimsSource -> claimsSource.supports(credentialType))
                .findFirst()
                .orElseThrow(() -> new IssuerServerException("server_error", "Unknown claims source for credential type [%s]".formatted(credentialType), HttpStatus.INTERNAL_SERVER_ERROR));
    }

    public final ClaimsSourceMetadata getMetadata(ClaimsSource claimsSource, CredentialConfigurationProperties credentialConfigurationProperties) {
        ClaimsSourceMetadata.ClaimsSourceMetadataBuilder builder = ClaimsSourceMetadata.builder();
        DocumentMetadata documentMetadata = claimsSource.getDocumentMetadata(new CredentialMetadataContext(credentialConfigurationProperties.getIdentifier(), credentialConfigurationProperties.getCredentialType(), credentialConfigurationProperties.getFormat()));
        builder.displays(documentMetadata.displays()
                .stream()
                .map(display ->
                        Display.builder()
                                .locale(display.locale())
                                .name(display.name())
                                .description(display.description())
                                .backgroundColor(display.backgroundColor())
                                .textColor(display.textColor())
                                .build())
                .toList());
        for (ClaimMetadata claimMetadata : documentMetadata.claims()) {
            builder.claim(ClaimsDescription.builder()
                    .path(claimMetadata.name())
                    .mandatory(claimMetadata.mandatory())
                    .displays(claimMetadata.displayNames()
                            .entrySet()
                            .stream()
                            .map(displayName -> Display.builder().locale(displayName.getKey()).name(displayName.getValue()).build()).toList())
                    .build());
        }
        return builder.build();
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
