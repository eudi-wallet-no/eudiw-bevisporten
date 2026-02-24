package no.idporten.eudiw.issuer.claimssource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.credentials.types.ClaimMetadata;
import no.idporten.eudiw.issuer.credentials.types.DocumentMetadata;
import no.idporten.eudiw.issuer.openid4vci.metadata.ClaimsDescription;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
@RequiredArgsConstructor
@Service
public class ClaimsSourceService implements InitializingBean {

    private final List<ClaimsSource> claimsSources;

    public ClaimsSource findClaimsSource(URI uri) {
        if (! "class".equals(uri.getScheme())) {
            throw new IssuerServerException("server_error", "Unsupported claims source URI scheme for uri [%s]".formatted(uri), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return claimsSources.stream()
                .filter(claimsSource -> claimsSource.getClass().getName().equals(uri.getAuthority()))
                .findFirst()
                .orElseThrow(() -> new IssuerServerException("server_error", "Unknown claims source for uri [%s]".formatted(uri), HttpStatus.INTERNAL_SERVER_ERROR));
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
                    .path(claimMetadata.path())
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
    public void afterPropertiesSet() {
        log.info("Claims source service managing {} claims sources", claimsSources.size());
        claimsSources.forEach(claimsSource -> {
            log.info("Claims source [{}]", claimsSource.getClass().getName());
        });
    }

}
