package no.idporten.eudiw.issuer.claimssource.pid;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PersonConverterService {

    // Convert ISO 3166-1 Alpha 3 to Alpha 2
    private final SortedMap<String, String> iso3166_1Alpha3ToAlpha2Map = new TreeMap<>();

    public PersonConverterService() {
        this.iso3166_1Alpha3ToAlpha2Map.putAll(createISO3166ConversionMap());
    }


    public boolean calcAgeOver18(final String fodselsdato) {
        LocalDate birthDate = LocalDate.parse(fodselsdato);
        LocalDate today = LocalDate.now();
        Period age = Period.between(birthDate, today);
        return age.getYears() >= 18;
    }

    public String calcPidExpiryDate() {

        int validityYears = 10; // PID administrative validity period in years
        ZonedDateTime utcNow = ZonedDateTime.now(ZoneOffset.UTC);
        return utcNow.plusYears(validityYears).truncatedTo(ChronoUnit.DAYS).toString();
    }

    public String getNationalityAlpha2(String nationalityAlpha3) {
        return iso3166_1Alpha3ToAlpha2Map.get(nationalityAlpha3);
    }
    public String getFirstNationalityAlpha2(List<String> nationalitiesAlpha3) {
        String nationality = nationalitiesAlpha3.getFirst();
        return iso3166_1Alpha3ToAlpha2Map.get(nationality);
    }


    // stolen from https://github.com/felleslosninger/idporten-c2id-server/blob/main/idporten-folkeregister-claims-source/src/main/java/no/idporten/c2id/server/spi/folkeregister/IDPortenFregClaimsSource.java#L87
    protected static Map<String, String> createISO3166ConversionMap() {
        return Arrays.stream(Locale.getAvailableLocales())
                .filter(locale -> {
                    try {
                        return !locale.getISO3Country().isEmpty();
                    } catch (MissingResourceException e) {
                        return false;
                    }
                })
                .collect(Collectors.toMap(Locale::getISO3Country, Locale::getCountry, (key, duplicate) -> key));
    }

    public String truncateTo150(String value) {
        if (value != null && value.length() > 150) {
            return value.substring(0, 150);
        } else {
            return value;
        }
    }
}
