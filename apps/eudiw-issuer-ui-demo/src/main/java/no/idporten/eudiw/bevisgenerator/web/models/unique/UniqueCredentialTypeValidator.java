package no.idporten.eudiw.bevisgenerator.web.models.unique;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import no.idporten.eudiw.bevisgenerator.integration.byobservice.ByobService;
import org.springframework.stereotype.Component;

@Component
public class UniqueCredentialTypeValidator implements ConstraintValidator<UniqueCredentialType, String> {

    private final ByobService repository;

    public UniqueCredentialTypeValidator(ByobService repository) {
        this.repository = repository;
    }

    @Override
    public boolean isValid(String credentialType, ConstraintValidatorContext context) {
        if (credentialType == null || credentialType.isBlank()) {
            return true;
        }
        String fullCredentialType = "net.eidas2sandkasse:" + credentialType;
        return !repository.existsByCredentialType(fullCredentialType);
    }
}

