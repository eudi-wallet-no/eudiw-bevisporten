package no.idporten.eudiw.issuer.cache;

import tools.jackson.databind.DatabindContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

// tools.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator is package protected
class LaissezFaireSubTypeValidator extends PolymorphicTypeValidator.Base {
    private static final long serialVersionUID = 1L;

    @Override
    public Validity validateBaseType(DatabindContext context, JavaType baseType) {
        return Validity.INDETERMINATE;
    }

    @Override
    public Validity validateSubClassName(DatabindContext context, JavaType baseType, String subClassName) {
        return Validity.ALLOWED;
    }

    @Override
    public Validity validateSubType(DatabindContext context, JavaType baseType, JavaType subType) {
        return Validity.ALLOWED;
    }
}
