package no.idporten.eudiw.issuer.ui.web;

import java.io.Serializable;

public record Issuance(String issuanceResponse, String encodedUri, String qrCode) implements Serializable {
}
