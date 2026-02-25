package no.idporten.eudiw.issuer.authoritativesources.pid;

import no.digdir.freg.service.FregService;
import org.springframework.stereotype.Service;

/**
 * PID variants for MDoc - https://github.com/eu-digital-identity-wallet/eudi-doc-attestation-rulebooks-catalog/blob/main/rulebooks/pid/pid-rulebook.md#3-isoiec-18013-5-compliant-encoding-of-pid .
 */
@Service
public class PIDMdocClaimsSource extends PIDClaimsSource {

    public static final String NAMESPACE = "eu.europa.ec.eudi.pid.1";

    public PIDMdocClaimsSource(FregService fregService, PersonConverterService personConverterService) {
        super(fregService, personConverterService);
    }

    /**
     * Namespace override for mdoc.
     * @return mdoc credential namespace
     */
    @Override
    protected String getNamespace() {
        return NAMESPACE;
    }

    /**
     * Attribute name overrides for PID in mdoc format.
     */
    @Override
    protected String calculateAttributeIdentifier(String dataIdentifier) {
        if ("birth_place".equals(dataIdentifier)) {
            return "place_of_birth";
        }
        return super.calculateAttributeIdentifier(dataIdentifier);
    }

}
