package no.idporten.eudiw.connector.authoritativesources.skatteetaten.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InntektsOpplysninger(
        @JsonProperty("inntekt")
        List<Inntekt> inntekt,
        @JsonProperty("kalendermaaned")
        String kalendermaaned
) {

    public static long sumFastlonn(InntektsOpplysninger inntektsOpplysninger) {
        return (long) inntektsOpplysninger.inntekt().stream().mapToDouble(Inntekt::beloep).sum();
    }
}
