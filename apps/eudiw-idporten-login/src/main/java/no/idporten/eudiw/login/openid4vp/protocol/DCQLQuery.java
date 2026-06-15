package no.idporten.eudiw.login.openid4vp.protocol;

public class DCQLQuery extends ProtocolJSONObject {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final DCQLQuery dcqlQuery = new DCQLQuery();

        public Builder credential(Credential credential) {
            dcqlQuery.appendArrayField("credentials", credential);
            return this;
        }

        public DCQLQuery build() {
            return dcqlQuery;
        }
    }

}
