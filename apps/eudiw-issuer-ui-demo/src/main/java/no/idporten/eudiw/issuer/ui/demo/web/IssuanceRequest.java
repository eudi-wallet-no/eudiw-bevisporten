package no.idporten.eudiw.issuer.ui.demo.web;

import java.io.Serializable;

public record IssuanceRequest(String body, String endpoint, String token, String headers) implements Serializable {
}
