package no.idporten.eudiw.statuslist.repository;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class StatusListPropertiesTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldPassValidation() {
        var props = new StatusListProperties(1000, 4);
        Set<ConstraintViolation<StatusListProperties>> violations = validator.validate(props);
        assertEquals(0, violations.size());
    }

    @Test
    void shouldFailWhenSizeTooSmall() {
        var props = new StatusListProperties(0, 4);
        Set<ConstraintViolation<StatusListProperties>> violations = validator.validate(props);
        assertEquals(1, violations.size());
        assertEquals("listSize", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void shouldFailWhenBitsPerEntryExceedsMax() {
        var props = new StatusListProperties(1, 9);
        Set<ConstraintViolation<StatusListProperties>> violations = validator.validate(props);
        assertEquals(1, violations.size());
        assertEquals("bitsPerStatus", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void shouldFailMultipleConstraints() {
        var props = new StatusListProperties(0, 0);
        Set<ConstraintViolation<StatusListProperties>> violations = validator.validate(props);
        assertEquals(2, violations.size());
    }
}