package no.idporten.eudiw.issuer.ui.demo.byob.model;


import com.fasterxml.jackson.annotation.JsonProperty;

public record Display(
        String name,
        String locale,

        @JsonProperty("background_color")
        String backgroundColor,

        @JsonProperty("text_color")
        String textColor
) {}