package no.idporten.eudiw.issuer.ui.demo.web;

public record AddCredentialForm(String json, String personIdentifier) {
    public AddCredentialForm() {
        this("", "");
    }
}
