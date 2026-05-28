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
    void shouldSucceedWhenBitsPerEntryIsValid() {
        int[] validBitsPerStatus = new int[] { 1, 2, 4, 8 };

        for (int bitsPerStatus : validBitsPerStatus) {
            StatusListProperties props = new StatusListProperties(1000, bitsPerStatus);
            assertEquals(bitsPerStatus, props.bitsPerStatus());
            Set<ConstraintViolation<StatusListProperties>> violations = validator.validate(props);
            assertEquals(0, violations.size());
        }
    }

    @Test
    void shouldFailWhenSizeTooSmall() {
        var props = new StatusListProperties(0, 4);
        Set<ConstraintViolation<StatusListProperties>> violations = validator.validate(props);
        assertEquals(1, violations.size());
        assertEquals("listSize", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void shouldFailMultipleConstraints() {
        var props = new StatusListProperties(0, 0);
        Set<ConstraintViolation<StatusListProperties>> violations = validator.validate(props);
        assertEquals(2, violations.size());
    }

    @Test
    void shouldFailWhenBitsPerEntryIsInvalid() {
        int[] invalidBitsPerStatus = new int[] { 0, 3, 5, 7, 9 };

        for (int bitsPerStatus : invalidBitsPerStatus) {
            var props = new StatusListProperties(1000, bitsPerStatus);
            Set<ConstraintViolation<StatusListProperties>> violations = validator.validate(props);
            assertEquals(1, violations.size());
            assertEquals("bitsPerStatus must be 1, 2, 4 or 8", violations.iterator().next().getMessage());
            assertEquals("bitsPerStatus", violations.iterator().next().getPropertyPath().toString());
        }
   }
}