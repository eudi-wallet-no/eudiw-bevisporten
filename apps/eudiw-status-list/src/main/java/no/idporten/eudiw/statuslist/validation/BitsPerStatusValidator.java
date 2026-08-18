package no.idporten.eudiw.statuslist.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class BitsPerStatusValidator implements ConstraintValidator<ValidBitsPerStatus, Integer> {
    private static final Set<Integer> VALID_BITS = Set.of(1, 2, 4, 8);

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        return value != null && VALID_BITS.contains(value);
    }
}
