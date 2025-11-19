package no.idporten.eudiw.issuer.claimssource.skatteetaten.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InntektsOpplysninger(
        List<Inntekt> inntekt,
        String kalendermaaned
) {
}
