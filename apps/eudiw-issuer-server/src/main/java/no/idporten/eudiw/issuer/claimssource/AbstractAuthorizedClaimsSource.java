package no.idporten.eudiw.issuer.claimssource;

public abstract non-sealed class AbstractAuthorizedClaimsSource implements AuthorizedClaimsSource {

    private ClaimsSourceProperties properties;

    @Override
    public void init(ClaimsSourceProperties properties) {
        this.properties = properties;
    }

    @Override
    public final ClaimsSourceProperties getProperties() {
        return properties;
    }
}
