package no.idporten.eudiw.bevisgenerator.web.models.unique;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UniqueVctValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueVct {

    String message() default "VCT finnes allerede";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
