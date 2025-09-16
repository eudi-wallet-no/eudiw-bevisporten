package no.idporten.eudiw.issuer.claimssource.pid;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
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
        return LocalDate.now().plusDays(90).format(DateTimeFormatter.ISO_LOCAL_DATE);
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
}
