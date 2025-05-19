package no.idporten.eudiw.issuer.claimssource;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.openid4vci.protocol.Credential;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CredentialIssuerService {

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;
    private final ClaimsSourceService claimsSourceService;

    public List<Credential> issueCredentials(CredentialRequest credentialRequest) {
        CredentialConfigurationProperties credentialConfigurationProperties = credentialIssuerServerProperties.findCredentialConfiguration(credentialRequest.getCredentialConfigurationId());
        ClaimsSource claimsSource = claimsSourceService.findCredentialClaimsSource(credentialConfigurationProperties.getDoctype());
        List<Claim> claims = claimsSource.retrieveClaims();
        return List.of(new Credential(claims.stream().map(claim -> claim.getPath() + "=" + claim.getValue()).collect(Collectors.joining(","))));
    }

}
