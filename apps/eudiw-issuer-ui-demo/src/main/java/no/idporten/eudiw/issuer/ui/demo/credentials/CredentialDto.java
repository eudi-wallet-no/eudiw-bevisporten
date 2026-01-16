package no.idporten.eudiw.issuer.ui.demo.credentials;


public record CredentialDto(String vct, String name, String json) {
    public CredentialDto {
        name = name == null ? cleanVct(vct) : name;
    }

    public CredentialDto(String vct, String json) {
        this(vct, null, json);
    }

    private static String cleanVct(String vct) {
        return vct.replaceFirst("^[^:]+:", "");
    }
}
