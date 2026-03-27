package no.idporten.eudiw.issuer.credentials;

import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceFormatException;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedClaimsDescription;
import no.idporten.eudiw.issuer.credentials.types.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;
import static no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceFormatException.INVALID_CLAIMS_DATA;

// Expects values to be non-null and of valid format
public class ClaimValueConverter {

    public static final DateTimeFormatter FORMATTER_ISO_DATE = ISO_LOCAL_DATE;
    public static final DateTimeFormatter FORMATTER_ISO_DATE_TIME = new DateTimeFormatterBuilder().parseCaseInsensitive().append(ISO_LOCAL_DATE).appendLiteral("T")
            .append(ISO_LOCAL_TIME).appendOffsetId().toFormatter();

    public Claim fullDateClaim(String key, String value) {
        try {
            LocalDate date = LocalDate.parse(value, FORMATTER_ISO_DATE);
            return buildClaim(key, new FullDateValue(date));
        } catch (DateTimeParseException e) {
            throw new ClaimsSourceFormatException(INVALID_CLAIMS_DATA, "Invalid format for claim %s from authoritative source".formatted(key), e);
        }
    }

    public Claim fullDateClaim(List<String> path, String value) {
        try {
            LocalDate date = LocalDate.parse(value, FORMATTER_ISO_DATE);
            return buildClaim(path, new FullDateValue(date));
        } catch (DateTimeParseException e) {
            throw new ClaimsSourceFormatException(INVALID_CLAIMS_DATA, "Invalid format for claim %s from authoritative source".formatted(path), e);
        }
    }

    public Claim fullDateClaim(String key, LocalDate value) {
        return buildClaim(key, new FullDateValue(value));
    }

    public Claim fullDateClaim(List<String> path, LocalDate value) {
        return buildClaim(path, new FullDateValue(value));
    }

    public Claim getDateTimeClaim(List<String> path, String value) {
        try {
            ZonedDateTime date = ZonedDateTime.parse(value, FORMATTER_ISO_DATE_TIME);
            return buildClaim(path, new DateTimeValue(date));
        } catch (DateTimeParseException e) {
            throw new ClaimsSourceFormatException(INVALID_CLAIMS_DATA, "Invalid format for claim %s from authoritative source".formatted(path), e);
        }
    }

    public Claim getDateTimeClaim(String key, String value) {
        try {
            ZonedDateTime date = ZonedDateTime.parse(value, FORMATTER_ISO_DATE_TIME);
            return buildClaim(key, new DateTimeValue(date));
        } catch (DateTimeParseException e) {
            throw new ClaimsSourceFormatException(INVALID_CLAIMS_DATA, "Invalid format for claim %s from authoritative source".formatted(key), e);
        }
    }

    public Claim getDateTimeClaim(String key, ZonedDateTime value) {
        return buildClaim(key, new DateTimeValue(value));
    }

    public Claim getDateTimeClaim(List<String> path, ZonedDateTime value) {
        return buildClaim(path, new DateTimeValue(value));
    }

    private static Claim buildClaim(String key, ClaimValue claimValue) {
        return Claim.builder().path(key).value(claimValue).build();
    }

    private static Claim buildClaim(List<String> path, ClaimValue claimValue) {
        return Claim.builder().path(path).value(claimValue).build();
    }

    public Claim getStringClaim(String key, String value) {
        return buildClaim(key, new StringValue(value));
    }

    public Claim getStringClaim(List<String> path, String value) {
        return buildClaim(path, new StringValue(value));
    }

    public Claim getBooleanClaim(String key, Boolean value) {
        return buildClaim(key, new BooleanValue(value));
    }

    public Claim getBooleanClaim(List<String> path, Boolean value) {
        return buildClaim(path, new BooleanValue(value));
    }

    public Claim getNumberClaim(String key, Integer value) {
        return buildClaim(key, new NumberValue(value));
    }

    public Claim getNumberClaim(List<String> path, Integer value) {
        return buildClaim(path, new NumberValue(value));
    }

    // Only support List of StringValue for now
    public Claim getListClaim(String key, List<String> value) {
        List<ClaimValue> list = new ArrayList<>();
        for (String v : value) {
            list.add(new StringValue(v));
        }
        return buildClaim(key, new ListValue(list));
    }

    // Only support List of StringValue for now
    public Claim getListClaim(List<String> path, List<String> value) {
        List<ClaimValue> list = new ArrayList<>();
        for (String v : value) {
            list.add(new StringValue(v));
        }
        return buildClaim(path, new ListValue(list));
    }

    // Only support Map of values of type StringValue for now
    public Claim getMapClaim(String key, Map<String, Object> value) {
        Map<String, ClaimValue> map = new HashMap<>();
        for (String k : value.keySet()) {
            map.put(k, claimValue(value.get(k)));
        }
        return buildClaim(key, new MapValue(map));
    }

    // Only support Map of values of type StringValue for now
    public Claim getMapClaim(List<String> path, Map<String, Object> value) {
        Map<String, ClaimValue> map = new HashMap<>();
        for (String k : value.keySet()) {
            map.put(k, claimValue(value.get(k)));
        }
        return buildClaim(path, new MapValue(map));
    }

    // experiment for map value types
    ClaimValue claimValue(Object value) {
        switch(value) {
            case String s -> {
                return new StringValue(s);
            }
            case Long l -> {
                return new NumberValue(l);
            }
            case Integer i -> {
                return new NumberValue(i);
            }
            case Boolean b -> {
                return new BooleanValue(b);
            }
            default -> throw new IllegalStateException("Map type %s not implemented yet".formatted(value));
        }
    }

    public Claim getBinaryClaim(String name, String base64Image, String mimeType) {
        return buildClaim(name, new BinaryValue(base64Image, mimeType));
    }

    public Claim getBinaryClaim(List<String> path, String base64Image, String mimeType) {
        return buildClaim(path, new BinaryValue(base64Image, mimeType));
    }

    // If type=null, use String as default.
    public Claim convertClaim(ExtendedClaimsDescription claim, Map<String, Object> storedClaims) {
        if (claim.type() == null) {
            return getStringClaim(claim.path(), (String) storedClaims.get(claim.name()));
        }
        return switch (claim.type()) {
            case ClaimDataType.STRING -> getStringClaim(claim.path(), (String) storedClaims.get(claim.name()));
            case ClaimDataType.NUMBER -> getNumberClaim(claim.path(), (Integer) storedClaims.get(claim.name()));
            case ClaimDataType.BOOLEAN -> getBooleanClaim(claim.path(), (Boolean) storedClaims.get(claim.name()));
            case ClaimDataType.ISO_DATE -> {
                if (storedClaims.get(claim.name()) instanceof LocalDate localDate) {
                    yield fullDateClaim(claim.path(), localDate);
                } else {
                    yield fullDateClaim(claim.path(), (String) storedClaims.get(claim.name()));
                }
            }
            case ClaimDataType.ISO_DATE_TIME -> {
                if (storedClaims.get(claim.name()) instanceof ZonedDateTime zonedDateTime) {
                    yield getDateTimeClaim(claim.path(), zonedDateTime);
                } else {
                    yield getDateTimeClaim(claim.path(), (String) storedClaims.get(claim.name()));
                }
            }
            case ClaimDataType.BINARY ->
                    getBinaryClaim(claim.path(), (String) storedClaims.get(claim.name()), claim.mimeType());
            case ClaimDataType.MAP ->
                    getMapClaim(claim.path(), (Map<String, Object>) storedClaims.get(claim.name())); // TODO handle errors better
            case ClaimDataType.LIST -> getListClaim(claim.path(), (List<String>) storedClaims.get(claim.name()));
        };
    }
}
