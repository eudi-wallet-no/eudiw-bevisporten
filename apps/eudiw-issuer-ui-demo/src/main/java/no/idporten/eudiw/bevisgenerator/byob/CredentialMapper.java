package no.idporten.eudiw.bevisgenerator.byob;

import no.idporten.eudiw.bevisgenerator.integration.byobservice.model.CredentialDefinition;
import no.idporten.eudiw.bevisgenerator.exception.IssuerUiException;
import no.idporten.eudiw.bevisgenerator.web.models.CredentialDto;
import no.idporten.eudiw.bevisgenerator.web.models.advancedForm.SimpleCredentialForm;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class CredentialMapper {

    ObjectMapper objectMapper;

    public CredentialMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CredentialDto toDto(CredentialDefinition cd) {
        try {
            return new CredentialDto(cd.getVct(), objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cd));
        } catch (JacksonException e) {
            throw new IssuerUiException("Failed converting to CredentialDto from CredentialDefinition from %s".formatted(cd), e);
        }
    }

    public CredentialDefinition fromDto(CredentialDto dto) {
        try {
            return objectMapper.readValue(dto.json(), CredentialDefinition.class);
        } catch (JacksonException e) {
            throw new IssuerUiException("Failed converting to CredentialDefinition from CredentialDto.json=%s".formatted(dto.json()), e);
        }
    }

    public CredentialDefinition fromDto(SimpleCredentialForm dto) {
       return new CredentialDefinition(dto);
    }

    public SimpleCredentialForm toSimpleCredentialForm(CredentialDefinition cd) {
       return new SimpleCredentialForm(cd);
    }
}
