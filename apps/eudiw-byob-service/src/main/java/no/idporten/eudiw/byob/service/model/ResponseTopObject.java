package no.idporten.eudiw.byob.service.model;

import java.util.List;

public record ResponseTopObject(
        List<ByobInput> byobs
) {
}
