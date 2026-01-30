package no.idporten.eudiw.issuer.claimssource;

import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimDataType;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public class ClaimDataTypeValidator {

    public void validate(ClaimMetadata claimMetadata, Object claim) {
        if (claim == null) {
            if(claimMetadata.mandatory()){
                throw new IssuerServerException("invalid_request", "Missing required value for claim %s".formatted(claimMetadata.name()), HttpStatus.BAD_REQUEST);
            }
            return;
        }

        switch (claimMetadata.type()) {
            case STRING -> validateStringInputValue(claimMetadata, ClaimDataType.STRING, claim);
            case NUMBER -> validateNumberValue(claimMetadata, claim);
            case BOOLEAN -> validateBooleanValue(claimMetadata, claim);
            case ISO_DATE -> validateIsoDateValue(claimMetadata, claim);
            case ISO_DATE_TIME -> validateIsoDateTimeValue(claimMetadata, claim);
            case BINARY -> validateStringInputValue(claimMetadata, ClaimDataType.BINARY, claim);
            case LIST -> validateListValue(claimMetadata, claim);
            case MAP -> validateMapValue(claimMetadata, claim);
        }
    }

    private static void validateListValue(ClaimMetadata claimMetadata, Object claim) {
        if (!(claim instanceof List<?> list)) {
            return;
        }
        final List<Object> value = (List<Object>) list;
        if (claimMetadata.mandatory() && value.isEmpty()) {
            throw new IssuerServerException("invalid_request", "Missing required value for list claim %s".formatted(claimMetadata.name()), HttpStatus.BAD_REQUEST);
        }
    }

    private static void validateMapValue(ClaimMetadata claimMetadata, Object claim) {
        if (!(claim instanceof Map<?, ?> map)) {
            return;
        }
        final Map<String, Object> value = (Map<String, Object>) map;
        if (claimMetadata.mandatory() && value.isEmpty()) {
            throw new IssuerServerException("invalid_request", "Missing required value for map claim %s".formatted(claimMetadata.name()), HttpStatus.BAD_REQUEST);
        }
    }

    private void validateIsoDateTimeValue(ClaimMetadata claimMetadata, Object claim) {
        if (claim instanceof ZonedDateTime) { // TODO: pga programmerte bevis, fjerne seinare?
            return;
        }

        if (!(claim instanceof String value)) {
            throw new IssuerServerException("invalid_request", "Invalid type for iso_date_time claim %s".formatted(claimMetadata.name()), HttpStatus.BAD_REQUEST);
        }
        if (!value.matches(claimMetadata.validationRegex())) {
            throw new IssuerServerException("invalid_request", "Invalid format for required value for iso_date_time claim %s".formatted(claimMetadata.name()), HttpStatus.BAD_REQUEST);
        }
    }

    private static void validateIsoDateValue(ClaimMetadata claimMetadata, Object claim) {
        // "2220-01-31"
        if (claim instanceof LocalDate) { // TODO: pga programmerte bevis, fjerne seinare?
            return;
        }

        if (!(claim instanceof String value)) {
            throw new IssuerServerException("invalid_request", "Invalid type for iso_date claim %s".formatted(claimMetadata.name()), HttpStatus.BAD_REQUEST);
        }
        if (!value.matches(claimMetadata.validationRegex())) {
            throw new IssuerServerException("invalid_request", "Invalid format for required value for iso_date claim %s".formatted(claimMetadata.name()), HttpStatus.BAD_REQUEST);
        }
    }

    private void validateBooleanValue(ClaimMetadata claimMetadata, Object claim) {

        if (!(claim instanceof Boolean)) {
            throw new IssuerServerException("invalid_request", "Invalid type for boolean claim %s".formatted(claimMetadata.name()), HttpStatus.BAD_REQUEST);
        }
    }

    private void validateNumberValue(ClaimMetadata claimMetadata, Object claim) {

        if (!(claim instanceof Integer)) {
            throw new IssuerServerException("invalid_request", "Invalid type for number claim %s".formatted(claimMetadata.name()), HttpStatus.BAD_REQUEST);
        }
    }

    private static void validateStringInputValue(ClaimMetadata claimMetadata, ClaimDataType dataType, Object claim) {
        if (!(claim instanceof String value)) {
            throw new IssuerServerException("invalid_request", "Invalid type for %s claim %s".formatted(dataType, claimMetadata.name()), HttpStatus.BAD_REQUEST);
        }
        if (claimMetadata.mandatory() && !StringUtils.hasLength(value)) {
            throw new IssuerServerException("invalid_request", "Missing required value for %s claim %s".formatted(dataType, claimMetadata.name()), HttpStatus.BAD_REQUEST);
        }
        if (!claimMetadata.mandatory() && !StringUtils.hasLength(value)) {
            return;
        }
        if (!value.matches(claimMetadata.validationRegex())) {
            throw new IssuerServerException("invalid_request", "Invalid format for required value for %s claim %s".formatted(dataType, claimMetadata.name()), HttpStatus.BAD_REQUEST);
        }
    }
}
