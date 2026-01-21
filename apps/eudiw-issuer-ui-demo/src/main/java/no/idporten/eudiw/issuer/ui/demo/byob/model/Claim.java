package no.idporten.eudiw.issuer.ui.demo.byob.model;


import java.util.List;

public record Claim(
        String path,
        String type,
        Boolean mandatory,
        List<Display> display
) {
    public Claim {
        if (mandatory == null) {
            mandatory = true;
        }
    }
}