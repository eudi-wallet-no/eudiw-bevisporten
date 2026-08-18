package no.idporten.eudiw.statuslist.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = BitsPerStatusValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBitsPerStatus {
    String message() default "bitsPerStatus must be 1, 2, 4 or 8";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
