package no.idporten.eudiw.byob.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Display(
        @NotBlank(message = "Display name is blank")
        @JsonProperty("name")
        String name,
        @NotBlank(message = "Display locale is blank")
        @JsonProperty("locale")
        String locale,
        @JsonProperty("background_color")
        String backgroundColor,
        @JsonProperty("text_color")
        String textColor
) {
}
