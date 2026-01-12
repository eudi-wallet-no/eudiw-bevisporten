package no.idporten.eudiw.issuer.openid4vci;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.api.Endpoints;
import no.idporten.eudiw.issuer.claimssource.ClaimsSource;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceMetadata;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceService;
import no.idporten.eudiw.issuer.claimssource.byob.DynamicCredentialConfigurationService;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServer;
import no.idporten.eudiw.issuer.oauth2.AuthorizationServerService;
import no.idporten.eudiw.issuer.openid4vci.metadata.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Configuration
public class CredentialIssuerServerConfiguration {

    private final ClaimsSourceService claimsSourceService;
    private final AuthorizationServerService authorizationServerService;
    private final DynamicCredentialConfigurationService dynamicCredentialConfigurationService;

    private static final Logger log = LoggerFactory.getLogger(CredentialIssuerServerConfiguration.class);

    @Bean
    public CredentialIssuerMetadata credentialIssuerMetadata(CredentialIssuerServerProperties credentialIssuerProperties) {
        CredentialIssuerMetadata.CredentialIssuerMetadataBuilder builder = CredentialIssuerMetadata.builder()
                .credentialIssuer(credentialIssuerProperties.getCredentialIssuer())
                .authorizationServers(credentialIssuerProperties.getAuthorizationServers().stream().map(AuthorizationServer::getIssuer).toList())
                .credentialEndpoint(Endpoints.endpointURI(credentialIssuerProperties.getCredentialIssuer(), Endpoints.CREDENTIAL_ENDPOINT))
                .nonceEndpoint(Endpoints.endpointURI(credentialIssuerProperties.getCredentialIssuer(), Endpoints.NONCE_ENDPOINT))
                .notificationEndpoint(Endpoints.endpointURI(credentialIssuerProperties.getCredentialIssuer(), Endpoints.NOTIFICATION_ENDPOINT))
                .displays(credentialIssuerProperties.getDisplayNames().keySet().stream().map(locale -> Display.builder().locale(locale).name(credentialIssuerProperties.getDisplayNames().get(locale)).build()).toList());
        List<CredentialConfigurationProperties> allCredentialConfigurationProperties = new ArrayList<>();
        allCredentialConfigurationProperties.addAll(credentialIssuerProperties.getCredentialConfigurations());
        allCredentialConfigurationProperties.addAll(getDynamicCredentialConfigurations());
        CredentialConfigurations credentialConfigurations = new CredentialConfigurations();
        for (CredentialConfigurationProperties credentialConfigurationProperties : allCredentialConfigurationProperties) {
            ClaimsSource claimsSource;
            ClaimsSourceMetadata claimsSourceMetadata;
            try {
                claimsSource = claimsSourceService.findClaimsSource(credentialConfigurationProperties.getCredentialType());
                claimsSourceMetadata = claimsSourceService.getMetadata(claimsSource, credentialConfigurationProperties);
            }catch (Exception e) {
                log.error("Error generating metadata for credential configuration id={} and credential type={}. Skipping this credential configuration in metadata response.", credentialConfigurationProperties.getIdentifier(), credentialConfigurationProperties.getCredentialType(), e);
                continue;
            }
            CredentialConfiguration.CredentialConfigurationBuilder credentialConfigurationBuilder = CredentialConfiguration.builder()
                    // credential-specific config
                    .format(credentialConfigurationProperties.getFormat().formatIdentifier())
                    .scope(credentialConfigurationProperties.getScope())
                    // config from claims source
                    .credentialMetadata(CredentialMetadata.builder()
                            .display(claimsSourceMetadata.getDisplays())
                            .claims(adjustClaimsDescriptionsToCredentialFormat(credentialConfigurationProperties.getFormat(), credentialConfigurationProperties.getCredentialType(), claimsSourceMetadata.getClaims()))
                            .build())
                    // config from issuer server
                    .cryptographicBindingMethods(credentialIssuerProperties.getCryptographicBindings())
                    .credentialSigningAlgValuesSupported(credentialIssuerProperties.getCredentialSigningAlgorithms())
                    .proofTypes(ProofTypes.builder().jwtProofType(JwtProofType.builder().algorithms(credentialIssuerProperties.getProofSigningAlgorithms()).build()).build());
            // config for formats
            if (CredentialFormat.MSO_MDOC.equals(credentialConfigurationProperties.getFormat())) {
                credentialConfigurationBuilder.doctype(credentialConfigurationProperties.getCredentialType());
            } else if (CredentialFormat.SD_JWT_VC.equals(credentialConfigurationProperties.getFormat())) {
                credentialConfigurationBuilder.vct(credentialConfigurationProperties.getCredentialType());
            }
            credentialConfigurations.put(credentialConfigurationProperties.getIdentifier(), credentialConfigurationBuilder.build());
        }

        builder.credentialConfigurations(credentialConfigurations);
        return builder.build();
    }

    private List<CredentialConfigurationProperties> getDynamicCredentialConfigurations() {
        try {
            return dynamicCredentialConfigurationService.generateCredentialConfigurations();
        } catch (RuntimeException e) {
            log.error("Failed to fetch dynamic credential configurations from BYOB service when generate metadata. Continuing without BYO-bevis. ", e);
            return Collections.emptyList();
        }
    }

    /**
     * Adjust claims description paths to credential format.
     * mdoc includes credential type in path.
     * SD-JWT VC adds claims descriptions for timestamps iat and exp.
     */
    List<ClaimsDescription> adjustClaimsDescriptionsToCredentialFormat(CredentialFormat credentialFormat, String credentialType, List<ClaimsDescription> claimsDescriptions) {
        List<ClaimsDescription> adjustedClaimsDescriptions = new ArrayList<>();
        adjustedClaimsDescriptions.addAll(claimsDescriptions
                .stream()
                .map(claimsDescription -> claimsDescription.forFormat(credentialFormat, credentialType))
                .toList());
        return adjustedClaimsDescriptions;
    }

    @Bean
    public RestClient preAuthorizationRestClient() {
        AuthorizationServer authorizationServer = authorizationServerService.getPrimaryAuthorizationServer();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.of(5, ChronoUnit.SECONDS));
        requestFactory.setReadTimeout(Duration.of(5, ChronoUnit.SECONDS));
        return
                RestClient.builder()
                        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .defaultHeader("X-API-KEY", authorizationServer.getApiKey())
                        .baseUrl(authorizationServer.getIssuer())
                        .requestFactory(requestFactory)
                        .build();
    }

}
