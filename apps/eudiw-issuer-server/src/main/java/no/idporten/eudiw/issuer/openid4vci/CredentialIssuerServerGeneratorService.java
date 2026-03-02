package no.idporten.eudiw.issuer.openid4vci;

import no.idporten.eudiw.issuer.claimssource.ClaimsSourceService;
import no.idporten.eudiw.issuer.config.CredentialConfigurationSource;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.config.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.credentials.formats.CredentialFormat;
import no.idporten.eudiw.issuer.credentials.types.ExtendedCredentialMetadata;
import no.idporten.eudiw.issuer.openid4vci.metadata.CredentialConfiguration;
import no.idporten.eudiw.issuer.openid4vci.metadata.CredentialConfigurations;
import no.idporten.eudiw.issuer.openid4vci.metadata.JwtProofType;
import no.idporten.eudiw.issuer.openid4vci.metadata.ProofTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CredentialIssuerServerGeneratorService {
    private final Logger log = LoggerFactory.getLogger(CredentialIssuerServerGeneratorService.class);

    private final ClaimsSourceService claimsSourceService;

    @Autowired
    public CredentialIssuerServerGeneratorService(ClaimsSourceService claimsSourceService) {
        this.claimsSourceService = claimsSourceService;
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
