package no.idporten.eudiw.bevisgenerator.integration.byobservice.model;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Claim(
        String path,
        String type,
        @JsonProperty("mime_type")
        String mimeType,
        Boolean mandatory,
        List<Display> display
) {
    public Claim {
        if (mandatory == null) {
            mandatory = true;
        }
    }

    public Claim (String path, String type, Boolean mandatory, List<Display> display) {
        this(path, type, null, mandatory, display);
    }
}