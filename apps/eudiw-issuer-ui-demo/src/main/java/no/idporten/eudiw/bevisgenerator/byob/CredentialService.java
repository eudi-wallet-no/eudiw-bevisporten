package no.idporten.eudiw.bevisgenerator.byob;

import no.idporten.eudiw.bevisgenerator.integration.byobservice.ByobService;
import no.idporten.eudiw.bevisgenerator.integration.byobservice.CredentialDefinitionFactory;
import no.idporten.eudiw.bevisgenerator.integration.byobservice.model.CredentialDefinition;
import no.idporten.eudiw.bevisgenerator.web.models.CredentialDto;
import no.idporten.eudiw.bevisgenerator.web.models.advancedForm.SimpleCredentialForm;
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

    public List<CredentialDto> getCredentialsForEdit() {
        List<CredentialDefinition> credentialDefinitions = byobService.getCredentialDefinitionsForEdit();

        return credentialDefinitions
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    public CredentialDto findCredential(String credentialType) {
        CredentialDefinition cd = byobService.getByCredentialType(credentialType);
        return mapper.toDto(cd);
    }

    public SimpleCredentialForm findSimpleCredential(String credentialType) {
        CredentialDefinition cd = byobService.getByCredentialType(credentialType);
        return mapper.toSimpleCredentialForm(cd);
    }

    public void storeCredential(CredentialDto dto) {
        CredentialDefinition cd =  mapper.fromDto(dto);

        cd.setCredentialType(dto.credentialType());
        byobService.addCredentialDefinition(cd);
    }

    public void storeCredential(SimpleCredentialForm formData) {
        CredentialDefinition cd = mapper.fromDto(formData);
        byobService.addCredentialDefinition(cd);
    }

    public void deleteCredential(String cvt) {
        byobService.removeCustomCredentialDefinition(cvt);
    }

    public void editCredential(CredentialDto dto) {
        CredentialDefinition cd =  mapper.fromDto(dto);

        cd.setCredentialType(dto.credentialType());
        byobService.editCredentialDefinition(cd);
    }

    public void editCredential(SimpleCredentialForm formData) {
        CredentialDefinition cd = mapper.fromDto(formData);
        byobService.editCredentialDefinition(cd);
    }

}
