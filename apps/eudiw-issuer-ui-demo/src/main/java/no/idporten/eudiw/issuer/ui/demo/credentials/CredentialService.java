package no.idporten.eudiw.issuer.ui.demo.credentials;

import no.idporten.eudiw.issuer.ui.demo.byob.ByobService;
import no.idporten.eudiw.issuer.ui.demo.byob.CredentialDefinitionFactory;
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

    public CredentialDto getEmptyCredentialDefinition() {
        return mapper.toDto(CredentialDefinitionFactory.empty());
    }

    public List<CredentialDto> getCredentials() {
        List<CredentialDefinition> credentialDefinitions = byobService.getCredentialDefinitions();

        return credentialDefinitions
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    public CredentialDto findCredential(String cvt) {
        CredentialDefinition cd = byobService.getByVct(cvt);
        return mapper.toDto(cd);
    }

    public void storeCredential(CredentialDto dto) {
        CredentialDefinition cd =  mapper.fromDto(dto);

        cd.setVct(dto.vct());
        byobService.addCredentialDefinition(cd);
    }

    public void deleteCredential(String cvt) {
        byobService.removeCustomCredentialDefinition(cvt);
    }

    public void editCredential(CredentialDto dto) {
        CredentialDefinition cd =  mapper.fromDto(dto);

        cd.setVct(dto.vct());
        byobService.editCredentialDefinition(cd);
    }
}
