package no.idporten.eudiw.issuer.ui.demo.credentials;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.idporten.eudiw.issuer.ui.demo.byob.model.CredentialDefinition;
import no.idporten.eudiw.issuer.ui.demo.exception.IssuerUiException;
import org.springframework.stereotype.Component;

@Component
public class CredentialMapper {

    ObjectMapper objectMapper;

    public CredentialMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CredentialDto toDto(CredentialDefinition cd) {
        try {
            return new CredentialDto(cd.getVct(), objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cd));
        } catch (JsonProcessingException e) {
            throw new IssuerUiException("Failed converting to CredentialDto from CredentialDefinition from %s".formatted(cd), e);
        }
    }

    public CredentialDefinition fromDto(CredentialDto dto) {
        try {
            return objectMapper.readValue(dto.json(), CredentialDefinition.class);
        } catch (JsonProcessingException e) {
            throw new IssuerUiException("Failed converting to CredentialDefinition from CredentialDto.json=%s".formatted(dto.json()), e);
        }
    }
}
