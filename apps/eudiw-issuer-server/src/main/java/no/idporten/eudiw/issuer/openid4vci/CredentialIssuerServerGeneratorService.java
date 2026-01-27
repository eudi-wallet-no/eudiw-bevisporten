package no.idporten.eudiw.issuer.openid4vci;

import no.idporten.eudiw.issuer.claimssource.ClaimsSource;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceMetadata;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceService;
import no.idporten.eudiw.issuer.claimssource.byob.DynamicCredentialConfigurationService;
import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicCredentialConfiguration;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.openid4vci.metadata.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CredentialIssuerServerGeneratorService {
    private final Logger log = LoggerFactory.getLogger(CredentialIssuerServerGeneratorService.class);

    private final CredentialIssuerServerProperties credentialIssuerProperties;
    private final ClaimsSourceService claimsSourceService;
    private final DynamicCredentialConfigurationService dynamicCredentialConfigurationService;

    @Autowired
    public CredentialIssuerServerGeneratorService(CredentialIssuerServerProperties credentialIssuerProperties, ClaimsSourceService claimsSourceService, DynamicCredentialConfigurationService dynamicCredentialConfigurationService) {
        this.claimsSourceService = claimsSourceService;
        this.dynamicCredentialConfigurationService = dynamicCredentialConfigurationService;
        this.credentialIssuerProperties = credentialIssuerProperties;
    }

    public Map<String, CredentialConfiguration> getByobCredentialConfigurations() {

        Map<String, DynamicCredentialConfiguration> configs;
        try {
            configs = dynamicCredentialConfigurationService.getDynamicCredentialConfigurations();
        } catch (RuntimeException e) {
            log.error("Failed to fetch dynamic credential configurations from BYOB service. Continue without BYO-bevis", e);
            return Collections.emptyMap();
        }
        Map<String, CredentialConfiguration> credentialConfigurations = new HashMap<>();
        CredentialConfigurationProperties propsTemplate = credentialIssuerProperties.getDynamicCredentialConfigurationTemplate();
        for (String credentialConfigurationId : configs.keySet()) {
            DynamicCredentialConfiguration dcc = configs.get(credentialConfigurationId);
            CredentialConfiguration credentialConfiguration = CredentialConfiguration.builder()
                    .vct(dcc.vct())
                    .format(dcc.format())
                    .scope(propsTemplate.getScope())
                    .credentialMetadata(CredentialMetadata.builder()
                            .display(dcc.getCredentialMetadata().displays().stream().map(this::convertToDisplay).toList())
                            .claims(convertClaimsToClaimsDescription(dcc.credentialMetadata().claims()))
                            .build())
                    .cryptographicBindingMethods(credentialIssuerProperties.getCryptographicBindings())
                    .credentialSigningAlgValuesSupported(credentialIssuerProperties.getCredentialSigningAlgorithms())
                    .proofTypes(ProofTypes.builder().jwtProofType(JwtProofType.builder().algorithms(credentialIssuerProperties.getProofSigningAlgorithms()).build()).build())
                    .build();
            credentialConfigurations.put(credentialConfigurationId, credentialConfiguration);
        }
        return credentialConfigurations;
    }

    private List<ClaimsDescription> convertClaimsToClaimsDescription(List<DynamicClaimMetadata> claimMetadata) {
        List<ClaimsDescription> claimsDescriptions = new ArrayList<>();
        for (DynamicClaimMetadata claimsMetadata : claimMetadata) {
            claimsDescriptions.add(convertToClaimsDescription(claimsMetadata));
        }
        return claimsDescriptions;
    }

    private ClaimsDescription convertToClaimsDescription(DynamicClaimMetadata claimsMetadata) {
        return ClaimsDescription.builder()
                .path(claimsMetadata.path())
                .mandatory(claimsMetadata.mandatory())
                .displays(convertDynamicDisplaysToDisplays(claimsMetadata.display()))
                .build();
    }

    private List<Display> convertDynamicDisplaysToDisplays(List<DocumentMetadata.Display> dynamicDisplays) {
        List<Display> displays = new ArrayList<>();
        for (DocumentMetadata.Display display : dynamicDisplays) {
            displays.add(convertToDisplay(display));
        }
        return displays;
    }

    private Display convertToDisplay(no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata.Display display) {
        return Display.builder()
                .locale(display.locale())
                .name(display.name())
                .build();
    }

    public CredentialConfigurations findCredentialConfigurations(CredentialIssuerServerProperties credentialIssuerProperties) {
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
            } catch (Exception e) {
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
                            .claims(claimsSourceMetadata.getClaims())
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
        return credentialConfigurations;

    }

    private List<CredentialConfigurationProperties> getDynamicCredentialConfigurations() {
        try {
            return dynamicCredentialConfigurationService.generateCredentialConfigurations();
        } catch (RuntimeException e) {
            log.error("Failed to fetch dynamic credential configurations from BYOB service when generate metadata. Continue without BYO-bevis. ", e);
            return Collections.emptyList();
        }
    }

}
