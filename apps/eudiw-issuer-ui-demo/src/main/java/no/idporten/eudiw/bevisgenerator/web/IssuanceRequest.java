package no.idporten.eudiw.bevisgenerator.web;

import java.io.Serializable;

public record IssuanceRequest(String body, String endpoint, String token, String headers) implements Serializable {
}
