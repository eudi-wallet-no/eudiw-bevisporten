package no.idporten.eudiw.issuer.claimssource.pid;

import no.digdir.freg.service.FregService;
import org.springframework.stereotype.Service;

/**
 * PID variants for MDoc - https://github.com/eu-digital-identity-wallet/eudi-doc-attestation-rulebooks-catalog/blob/main/rulebooks/pid/pid-rulebook.md#3-isoiec-18013-5-compliant-encoding-of-pid .
 */
@Service
public class PIDMdocClaimsSource extends PIDClaimsSource {


    public PIDMdocClaimsSource(FregService fregService, PersonConverterService personConverterService) {
        super(fregService, personConverterService);
    }

    /**
     * Attribute name overrides for PID in mdoc format.
     */
    @Override
    protected String getAttributeIdentifier(String dataIdentifier) {
        if ("birth_place".equals(dataIdentifier)) {
            return "place_of_birth";
        }
        return super.getAttributeIdentifier(dataIdentifier);
    }

    @Override
    protected String getCredentialName() {
        return super.getCredentialName() + " (mdoc)";
    }

}
