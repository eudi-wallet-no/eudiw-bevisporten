package no.idporten.eudiw.issuer.claimssource;

import no.idporten.eudiw.issuer.claimssource.domain.*;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceFormatException;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata.*;
import static no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata.TYPE_BINARY;
import static no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata.TYPE_DATETIME;
import static no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata.TYPE_FULLDATE;
import static no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceFormatException.INVALID_CLAIMS_DATA;

// Expects values to be non-null and of valid format
public class ClaimValueConverter {

    public Claim getFullDateClaim(String key, String value) {
        try {
            LocalDate date = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            return buildClaim(key, new FullDateValue(date));
        }catch(DateTimeParseException e){
            throw new ClaimsSourceFormatException(INVALID_CLAIMS_DATA,"Invalid format for claim %s from authoritative source".formatted(key), e);
        }
    }

    public Claim getFullDateClaim(List<String> path, String value) {
        try {
            LocalDate date = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            return buildClaim(path, new FullDateValue(date));
        }catch(DateTimeParseException e){
            throw new ClaimsSourceFormatException(INVALID_CLAIMS_DATA,"Invalid format for claim %s from authoritative source".formatted(path), e);
        }
    }

    public Claim getFullDateClaim(String key, LocalDate value) {
        return buildClaim(key, new FullDateValue(value));
    }

    public Claim getFullDateClaim(List<String> path, LocalDate value) {
        return buildClaim(path, new FullDateValue(value));
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
    public Claim getMapClaim(String key, Map<String, String> value) {
        Map<String, ClaimValue> map = new HashMap<>();
        for (String k : value.keySet()) {
            map.put(k, new StringValue(value.get(k)));
        }
        return buildClaim(key, new MapValue(map));
    }

    // Only support Map of values of type StringValue for now
    public Claim getMapClaim(List<String> path, Map<String, String> value) {
        Map<String, ClaimValue> map = new HashMap<>();
        for (String k : value.keySet()) {
            map.put(k, new StringValue(value.get(k)));
        }
        return buildClaim(path, new MapValue(map));
    }


    public Claim getBinaryClaim(String name, String base64Image) {
        byte[] imageAsBytes = Base64.getDecoder().decode(base64Image);
        return buildClaim(name, new BinaryValue(imageAsBytes));
    }

    public Claim getBinaryClaim(List<String> path, String base64Image) {
        byte[] imageAsBytes = Base64.getDecoder().decode(base64Image);
        return buildClaim(path, new BinaryValue(imageAsBytes));
    }

    public Claim getDataClaim(String key, String value) {
        return buildClaim(key, new DataValue(value));
    }

    public Claim getDataClaim(List<String> path, String value) {
        return buildClaim(path, new DataValue(value));
    }

    // Default implementation supporting basic claim types, List and Map later
    public Claim convertClaim(ClaimMetadata claim, Map<String, Object> storedClaims) {
        return switch (claim.type()) {
            case TYPE_STRING ->
                    getStringClaim(claim.path(), (String) storedClaims.get(claim.name()));
            case TYPE_NUMBER ->
                    getNumberClaim(claim.path(), (Integer) storedClaims.get(claim.name()));
            case TYPE_BOOLEAN ->
                    getBooleanClaim(claim.path(), (Boolean) storedClaims.get(claim.name()));
            case TYPE_FULLDATE -> {
                if (storedClaims.get(claim.name()) instanceof LocalDate localDate) {
                    yield getFullDateClaim(claim.path(), localDate);
                } else {
                    yield getFullDateClaim(claim.path(), (String) storedClaims.get(claim.name()));
                }
            }
            case TYPE_DATETIME ->
                    getDateTimeClaim(claim.path(), (ZonedDateTime) storedClaims.get(claim.name()));
            case TYPE_BINARY ->
                    getBinaryClaim(claim.path(), (String) storedClaims.get(claim.name()));
            case TYPE_DATA ->
                    getDataClaim(claim.path(), (String) storedClaims.get(claim.name()));
            default ->
                // Default to string claim for now
                    getStringClaim(claim.path(), (String) storedClaims.get(claim.name()));
        };
    }
}
