package no.idporten.eudiw.issuer.ui.web;

import java.io.Serializable;

public record IssuanceRequest(String body, String endpoint, String token, String headers) implements Serializable {
}
