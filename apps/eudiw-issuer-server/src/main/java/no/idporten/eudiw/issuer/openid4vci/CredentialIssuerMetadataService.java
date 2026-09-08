package no.idporten.eudiw.issuer.openid4vci;

import no.idporten.eudiw.issuer.api.Endpoints;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceService;
import no.idporten.eudiw.issuer.config.CredentialConfigurationSource;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenantService;
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
    private final CredentialIssuerTenantService credentialIssuerTenantService;
    private final CredentialIssuerServerProperties credentialIssuerServerProperties;

    private final Map<String, CredentialIssuerMetadata> credentialIssuerMetadataCache = new ConcurrentHashMap<>();


    @Autowired
    public CredentialIssuerMetadataService(ClaimsSourceService claimsSourceService, CredentialIssuerTenantService credentialIssuerTenantService, CredentialIssuerServerProperties credentialIssuerServerProperties) {
        this.claimsSourceService = claimsSourceService;
        this.credentialIssuerTenantService = credentialIssuerTenantService;
        this.credentialIssuerServerProperties = credentialIssuerServerProperties;
    }

    public CredentialIssuerMetadata getCredentialIssuerMetadata(String tenant) {
        final String normalizedTenant = credentialIssuerTenantService.normalizeTenant(tenant);
        CredentialIssuerMetadata credentialIssuerMetadata = credentialIssuerMetadataCache.get(normalizedTenant);
        if (credentialIssuerMetadata == null) {
            credentialIssuerMetadata = credentialIssuerMetadata(credentialIssuerServerProperties, credentialIssuerTenantService.findTenantById(normalizedTenant));
            credentialIssuerMetadataCache.put(normalizedTenant, credentialIssuerMetadata);
        }
        return credentialIssuerMetadata;
    }

    @Scheduled(initialDelay = 10 * 1000L, fixedRate = 30 * 1000L)
    public void refreshCredentialIssuerMetadata() {
        log.info("Refreshing credential issuer metadata for all credential issuers");
        for (CredentialIssuerTenant tenant : credentialIssuerTenantService.findAllTenants()) {
            credentialIssuerMetadataCache.put(tenant.getId(), credentialIssuerMetadata(credentialIssuerServerProperties, tenant));
            log.info("Refreshed credential issuer metadata for tenant {}", tenant.getId());
        }
    }

    public CredentialIssuerMetadata credentialIssuerMetadata(CredentialIssuerServerProperties credentialIssuerProperties, CredentialIssuerTenant tenant) {
        CredentialIssuerMetadata.CredentialIssuerMetadataBuilder builder = CredentialIssuerMetadata.builder()
                .credentialIssuer(tenant.getCredentialIssuer())
                .authorizationServers(credentialIssuerProperties.getAuthorizationServers().stream().map(AuthorizationServer::getIssuer).toList())
                .credentialEndpoint(Endpoints.endpointURI(credentialIssuerProperties.getCredentialIssuer(), Endpoints.CREDENTIAL_ENDPOINT_TENANT, tenant.getId()))
                .nonceEndpoint(Endpoints.endpointURI(credentialIssuerProperties.getCredentialIssuer(), Endpoints.NONCE_ENDPOINT_TENANT, tenant.getId()))
                .notificationEndpoint(Endpoints.endpointURI(credentialIssuerProperties.getCredentialIssuer(), Endpoints.NOTIFICATION_ENDPOINT_TENANT, tenant.getId()))
                .displays(tenant.getDisplayNames().keySet().stream().map(locale -> Display.builder().locale(locale).name(tenant.getDisplayNames().get(locale)).build()).toList());
        if (tenant.getBatchSize() >= 2) {
            builder.batchCredentialIssuance(BatchCredentialIssuance.builder().batchSize(tenant.getBatchSize()).build());
        }
        CredentialConfigurations credentialConfigurations = buildCredentialConfigurations(credentialIssuerServerProperties, tenant);
        builder.credentialConfigurations(credentialConfigurations);
        return builder.build();
    }

    private CredentialConfigurations buildCredentialConfigurations(CredentialIssuerServerProperties credentialIssuerProperties, CredentialIssuerTenant tenant) {
        List<ExtendedCredentialConfiguration> extendedCredentialConfigurations = new ArrayList<>();
        for (CredentialConfigurationSource credentialConfigurationSource : tenant.getCredentialConfigurationSources()) {
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
                    .cryptographicBindingMethods(credentialIssuerProperties.getCryptographicBindings());
            // config for formats
            if (CredentialFormat.MSO_MDOC.equals(credentialConfiguration.getFormat())) {
                credentialConfigurationBuilder
                        .doctype(credentialConfiguration.getCredentialType())
                        .credentialSigningAlgValuesSupportedMdoc(credentialIssuerProperties.getCredentialSigningAlgorithms().getMsoMdoc());
            } else if (CredentialFormat.SD_JWT_VC.equals(credentialConfiguration.getFormat())) {
                credentialConfigurationBuilder
                        .vct(credentialConfiguration.getCredentialType())
                        .credentialSigningAlgValuesSupported(credentialIssuerProperties.getCredentialSigningAlgorithms().getDcSdJwt());
            }
            KeyAttestationRequired keyAttestationRequired = credentialIssuerProperties.isKeyAttestationsRequired()
                    ? KeyAttestationRequired.builder()
                            .keyStorage(credentialIssuerProperties.getAttestationKeyStorage())
                            .userAuthentication(credentialIssuerProperties.getAttestationUserAuthentication())
                            .build()
                    : null;
            credentialConfigurationBuilder.proofTypes(ProofTypes.builder()
                    .jwtProofType(
                            ProofType.builder().algorithms(credentialIssuerProperties.getProofSigningAlgorithms())
                                    .keyAttestationsRequired(keyAttestationRequired)
                                    .build())
                            .attestationProofType(
                                    ProofType.builder()
                                            .algorithms(credentialIssuerProperties.getProofSigningAlgorithms())
                                            .keyAttestationsRequired(keyAttestationRequired)
                                            .build())
                            .build());
            credentialConfigurations.put(credentialConfiguration.getCredentialConfigurationId(), credentialConfigurationBuilder.build());
        }
        return credentialConfigurations;
    }

}
