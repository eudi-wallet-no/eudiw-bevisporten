package no.idporten.eudiw.issuer.openid4vci;

import no.idporten.eudiw.issuer.api.Endpoints;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceService;
import no.idporten.eudiw.issuer.config.CredentialConfigurationSource;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.credentials.formats.CredentialFormat;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialMetadata;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServer;
import no.idporten.eudiw.issuer.openid4vci.metadata.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for generating and caching credential issuer metadata for a credential issuer.  Uses the issuer's
 * configuration and its credential configuration sources to generate the metadata.
 */
@Service
public class CredentialIssuerMetadataService {
    private final Logger log = LoggerFactory.getLogger(CredentialIssuerMetadataService.class);

    private final ClaimsSourceService claimsSourceService;
    private final CredentialIssuerServerProperties credentialIssuerServerProperties;

    private Map<String, CredentialIssuerMetadata> credentialIssuerMetadataCache = new ConcurrentHashMap<>();


    @Autowired
    public CredentialIssuerMetadataService(ClaimsSourceService claimsSourceService, CredentialIssuerServerProperties credentialIssuerServerProperties) {
        this.claimsSourceService = claimsSourceService;
        this.credentialIssuerServerProperties = credentialIssuerServerProperties;
    }

    public CredentialIssuerMetadata getCredentialIssuerMetadata(String credentialIssuer) {
        return credentialIssuerMetadataCache.get(credentialIssuer);
    }

    @Scheduled(initialDelay = 10 * 1000L, fixedRate = 30 * 1000L)
    public void refreshCredentialIssuerMetadata() {
        log.info("Refreshing credential issuer metadata for all credential issuers");
        credentialIssuerMetadataCache.put("root", credentialIssuerMetadata(credentialIssuerServerProperties));
    }

    public CredentialIssuerMetadata credentialIssuerMetadata(CredentialIssuerServerProperties credentialIssuerProperties) {
        CredentialIssuerMetadata.CredentialIssuerMetadataBuilder builder = CredentialIssuerMetadata.builder()
                .credentialIssuer(credentialIssuerProperties.getCredentialIssuer())
                .authorizationServers(credentialIssuerProperties.getAuthorizationServers().stream().map(AuthorizationServer::getIssuer).toList())
                .credentialEndpoint(Endpoints.endpointURI(credentialIssuerProperties.getCredentialIssuer(), Endpoints.CREDENTIAL_ENDPOINT))
                .nonceEndpoint(Endpoints.endpointURI(credentialIssuerProperties.getCredentialIssuer(), Endpoints.NONCE_ENDPOINT))
                .notificationEndpoint(Endpoints.endpointURI(credentialIssuerProperties.getCredentialIssuer(), Endpoints.NOTIFICATION_ENDPOINT))
                .displays(credentialIssuerProperties.getDisplayNames().keySet().stream().map(locale -> Display.builder().locale(locale).name(credentialIssuerProperties.getDisplayNames().get(locale)).build()).toList());
        CredentialConfigurations credentialConfigurations = findCredentialConfigurations(credentialIssuerProperties);
        builder.credentialConfigurations(credentialConfigurations);
        return builder.build();
    }

    public CredentialConfigurations findCredentialConfigurations(CredentialIssuerServerProperties credentialIssuerProperties) {
        List<ExtendedCredentialConfiguration> extendedCredentialConfigurations = new ArrayList<>();
        for (CredentialConfigurationSource credentialConfigurationSource : credentialIssuerProperties.getCredentialConfigurationSources()) {
            extendedCredentialConfigurations.addAll(credentialConfigurationSource.retrieve());
        }
        CredentialConfigurations credentialConfigurations = new CredentialConfigurations();
        for (ExtendedCredentialConfiguration credentialConfiguration : extendedCredentialConfigurations) {
            ExtendedCredentialMetadata claimsSourceMetadata = credentialConfiguration.getExtendedCredentialMetadata();
            try {
                claimsSourceService.findClaimsSource(credentialConfiguration.getCredentialIssuerContext().getCredentialDataSourceUri());
            } catch (Exception e) {
                log.error("Error generating metadata for credential configuration id={} and credential type={}. Skipping this credential configuration in metadata response.", credentialConfiguration.getCredentialConfigurationId(), credentialConfiguration.getCredentialType(), e);
                continue;
            }
            CredentialConfiguration.CredentialConfigurationBuilder credentialConfigurationBuilder = CredentialConfiguration.builder()
                    // credential-specific config
                    .format(credentialConfiguration.getFormat().formatIdentifier())
                    .scope(credentialConfiguration.getScope())
                    // metadata from extended internal model
                    .credentialMetadata(claimsSourceMetadata.toOpenID4VCICredentialMetadata())
                    // config from issuer server
                    .cryptographicBindingMethods(credentialIssuerProperties.getCryptographicBindings())
                    .credentialSigningAlgValuesSupported(credentialIssuerProperties.getCredentialSigningAlgorithms())
                    .proofTypes(ProofTypes.builder().jwtProofType(JwtProofType.builder().algorithms(credentialIssuerProperties.getProofSigningAlgorithms()).build()).build());
            // config for formats
            if (CredentialFormat.MSO_MDOC.equals(credentialConfiguration.getFormat())) {
                credentialConfigurationBuilder.doctype(credentialConfiguration.getCredentialType());
            } else if (CredentialFormat.SD_JWT_VC.equals(credentialConfiguration.getFormat())) {
                credentialConfigurationBuilder.vct(credentialConfiguration.getCredentialType());
            }
            credentialConfigurations.put(credentialConfiguration.getCredentialConfigurationId(), credentialConfigurationBuilder.build());
        }
        return credentialConfigurations;

    }

}
