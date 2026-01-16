package no.idporten.eudiw.issuer.ui.demo.credentials;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.idporten.eudiw.issuer.ui.demo.byob.model.CredentialDefinition;
import org.springframework.stereotype.Component;

@Component
public class CredentialMapper {

    ObjectMapper objectMapper;

    public CredentialMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CredentialDto toDto(CredentialDefinition cd) throws JsonProcessingException {
        return new CredentialDto(cd.getVct(), objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cd));
    }

    public CredentialDefinition fromDto(CredentialDto dto) throws JsonProcessingException {
        return objectMapper.readValue(dto.json(), CredentialDefinition.class);
    }
}
