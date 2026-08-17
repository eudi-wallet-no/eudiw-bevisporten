package no.idporten.eudiw.connector.authoritativesources;

public enum AuthoritativeSources {
    FREG,
    ADVOKATREGISTERET,
    KRR,
    SKATTEETATEN,
    BYOB,
    UNKNOWN;

    public String externalName() {
        return this.name().toLowerCase();
    }
}
