package no.idporten.eudiw.issuer.credentials;

import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.credentials.types.ClaimDataType;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedClaimsDescription;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class ClaimDataTypeValidator {

    public void validate(ExtendedClaimsDescription extendedClaimsDescription, Object claim) {
        if (claim == null) {
            if(extendedClaimsDescription.mandatory()){
                throw new IssuerServerException("invalid_request", "Missing required value for claim %s".formatted(extendedClaimsDescription.name()), HttpStatus.BAD_REQUEST);
            }
            return;
        }

        switch (extendedClaimsDescription.type()) {
            case STRING -> validateStringInputValue(extendedClaimsDescription, ClaimDataType.STRING, claim);
            case NUMBER -> validateNumberValue(extendedClaimsDescription, claim);
            case BOOLEAN -> validateBooleanValue(extendedClaimsDescription, claim);
            case ISO_DATE -> validateIsoDateValue(extendedClaimsDescription, claim);
            case ISO_DATE_TIME -> validateIsoDateTimeValue(extendedClaimsDescription, claim);
            case BINARY -> validateStringInputValue(extendedClaimsDescription, ClaimDataType.BINARY, claim);
            case LIST -> validateListValue(extendedClaimsDescription, claim);
            case MAP -> validateMapValue(extendedClaimsDescription, claim);
        }
    }

    private static void validateListValue(ExtendedClaimsDescription extendedClaimsDescription, Object claim) {
        if (!(claim instanceof List<?> list)) {
            return;
        }
        final List<Object> value = (List<Object>) list;
        if (extendedClaimsDescription.mandatory() && value.isEmpty()) {
            throw new IssuerServerException("invalid_request", "Missing required value for list claim %s".formatted(extendedClaimsDescription.name()), HttpStatus.BAD_REQUEST);
        }
    }

    private static void validateMapValue(ExtendedClaimsDescription extendedClaimsDescription, Object claim) {
        if (!(claim instanceof Map<?, ?> map)) {
            return;
        }
        final Map<String, Object> value = (Map<String, Object>) map;
        if (extendedClaimsDescription.mandatory() && value.isEmpty()) {
            throw new IssuerServerException("invalid_request", "Missing required value for map claim %s".formatted(extendedClaimsDescription.name()), HttpStatus.BAD_REQUEST);
        }
    }

    private void validateIsoDateTimeValue(ExtendedClaimsDescription extendedClaimsDescription, Object claim) {
        if (claim instanceof ZonedDateTime) { // TODO: pga programmerte bevis, fjerne seinare?
            return;
        }

        if (!(claim instanceof String value)) {
            throw new IssuerServerException("invalid_request", "Invalid type for iso_date_time claim %s".formatted(extendedClaimsDescription.name()), HttpStatus.BAD_REQUEST);
        }
        if (!value.matches(extendedClaimsDescription.validationRegex())) {
            throw new IssuerServerException("invalid_request", "Invalid format for required value for iso_date_time claim %s".formatted(extendedClaimsDescription.name()), HttpStatus.BAD_REQUEST);
        }
    }

    private static void validateIsoDateValue(ExtendedClaimsDescription extendedClaimsDescription, Object claim) {
        // "2220-01-31"
        if (claim instanceof LocalDate) { // TODO: pga programmerte bevis, fjerne seinare?
            return;
        }

        if (!(claim instanceof String value)) {
            throw new IssuerServerException("invalid_request", "Invalid type for iso_date claim %s".formatted(extendedClaimsDescription.name()), HttpStatus.BAD_REQUEST);
        }
        if (!value.matches(extendedClaimsDescription.validationRegex())) {
            throw new IssuerServerException("invalid_request", "Invalid format for required value for iso_date claim %s".formatted(extendedClaimsDescription.name()), HttpStatus.BAD_REQUEST);
        }
    }

    private void validateBooleanValue(ExtendedClaimsDescription extendedClaimsDescription, Object claim) {

        if (!(claim instanceof Boolean)) {
            throw new IssuerServerException("invalid_request", "Invalid type for boolean claim %s".formatted(extendedClaimsDescription.name()), HttpStatus.BAD_REQUEST);
        }
    }

    private void validateNumberValue(ExtendedClaimsDescription extendedClaimsDescription, Object claim) {

        if (!(claim instanceof Integer)) {
            throw new IssuerServerException("invalid_request", "Invalid type for number claim %s".formatted(extendedClaimsDescription.name()), HttpStatus.BAD_REQUEST);
        }
    }

    private static void validateStringInputValue(ExtendedClaimsDescription extendedClaimsDescription, ClaimDataType dataType, Object claim) {
        if (!(claim instanceof String value)) {
            throw new IssuerServerException("invalid_request", "Invalid type for %s claim %s".formatted(dataType, extendedClaimsDescription.name()), HttpStatus.BAD_REQUEST);
        }
        if (extendedClaimsDescription.mandatory() && !StringUtils.hasLength(value)) {
            throw new IssuerServerException("invalid_request", "Missing required value for %s claim %s".formatted(dataType, extendedClaimsDescription.name()), HttpStatus.BAD_REQUEST);
        }
        if (!extendedClaimsDescription.mandatory() && !StringUtils.hasLength(value)) {
            return;
        }
        if (!value.matches(extendedClaimsDescription.validationRegex())) {
            throw new IssuerServerException("invalid_request", "Invalid format for required value for %s claim %s".formatted(dataType, extendedClaimsDescription.name()), HttpStatus.BAD_REQUEST);
        }
    }
}
