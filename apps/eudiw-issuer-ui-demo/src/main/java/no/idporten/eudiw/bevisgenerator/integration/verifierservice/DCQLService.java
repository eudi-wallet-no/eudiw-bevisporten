package no.idporten.eudiw.bevisgenerator.integration.verifierservice;

import no.idporten.eudiw.bevisgenerator.integration.issuerserver.credentialdefinitionmodel.CredentialIssuerMetadata;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.CredentialDefinitionDisplayData;

import java.util.List;

public interface DCQLService {
    List<CredentialDefinitionDisplayData> createCredentialDefinitionDisplayData(List<CredentialIssuerMetadata> credentialIssuerMetadata);
    String buildDcql(CredentialDefinitionDisplayData definition, List<String> selectedClaimPaths);
}
