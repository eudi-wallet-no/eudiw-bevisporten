package no.idporten.eudiw.issuer.ui.demo.credentials.unique;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import no.idporten.eudiw.issuer.ui.demo.byob.ByobService;
import org.springframework.stereotype.Component;

@Component
public class UniqueVctValidator implements ConstraintValidator<UniqueVct, String> {

    private final ByobService repository;

    public UniqueVctValidator(ByobService repository) {
        this.repository = repository;
    }

    @Override
    public boolean isValid(String vct, ConstraintValidatorContext context) {
        if (vct == null || vct.isBlank()) {
            return true;
        }
        String fullVct = "net.eidas2sandkasse:" + vct;
        return !repository.existsByVct(fullVct);
    }
}

