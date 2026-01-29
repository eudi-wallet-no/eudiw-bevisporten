package no.idporten.eudiw.bevisgenerator.integration.byobservice.model;


import com.fasterxml.jackson.annotation.JsonProperty;

public record Display(
        String name,
        String locale,

        @JsonProperty("background_color")
        String backgroundColor,

        @JsonProperty("text_color")
        String textColor
) {
        public Display(String name) {
               this(name, "no", null, null);
        }
}