package no.idporten.eudiw.issuer.ui.demo.credentials;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.idporten.eudiw.issuer.ui.demo.byob.ByobService;
import no.idporten.eudiw.issuer.ui.demo.byob.model.CredentialDefinition;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CredentialService {
    private final ByobService byobService;
    private final CredentialMapper mapper;

    public CredentialService(ByobService byobService, CredentialMapper mapper) {
        this.byobService = byobService;
        this.mapper = mapper;
    }

    public List<CredentialDto> getCredentials() {
        List<CredentialDefinition> credentialDefinitions = byobService.getCredentialConfigurations();

        return credentialDefinitions.stream().map(cd -> {
            try {
                return mapper.toDto(cd);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }

    public CredentialDto findCredential(String cvt) throws JsonProcessingException {
        CredentialDefinition cd = byobService.getCredentialDefinitionByVct(cvt);
        return mapper.toDto(cd);
    }

    public void storeCredential(CredentialDto dto) throws JsonProcessingException {
        CredentialDefinition cd =  mapper.fromDto(dto);

        cd.setVct(dto.vct());
        byobService.addCredentialDefinition(cd);
    }

    public void deleteCredential(String cvt) {
        byobService.removeCustomCredentialDefinition(cvt);
    }

    public void editCredential(CredentialDto dto) throws JsonProcessingException {
        CredentialDefinition cd =  mapper.fromDto(dto);

        cd.setVct(dto.vct());
        byobService.editCredentialDefinition(cd);
    }
}
