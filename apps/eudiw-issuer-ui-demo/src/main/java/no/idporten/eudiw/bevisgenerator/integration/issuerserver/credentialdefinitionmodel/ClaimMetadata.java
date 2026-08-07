package no.idporten.eudiw.bevisgenerator.integration.issuerserver.credentialdefinitionmodel;

import no.idporten.eudiw.bevisgenerator.integration.byobservice.model.Display;

import java.util.List;

public record ClaimMetadata(
        List<String> path,
        Boolean mandatory,
        List<Display> display
) {
    public ClaimMetadata {
        if (mandatory == null) {
            mandatory = true;
        }
    }
}
