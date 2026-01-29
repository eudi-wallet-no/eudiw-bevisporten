package no.idporten.eudiw.bevisgenerator.web;

import java.io.Serializable;

public record Issuance(String issuanceResponse, String encodedUri, String qrCode) implements Serializable {
}
