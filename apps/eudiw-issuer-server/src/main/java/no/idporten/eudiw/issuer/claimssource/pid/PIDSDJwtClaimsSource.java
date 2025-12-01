package no.idporten.eudiw.issuer.claimssource.pid;

import no.digdir.freg.service.FregService;
import org.springframework.stereotype.Service;

/**
 * PID variants for SD-JWT VC - https://github.com/eu-digital-identity-wallet/eudi-doc-attestation-rulebooks-catalog/blob/main/rulebooks/pid/pid-rulebook.md#41-encoding-of-pid-attributes-and-metadata
 */
@Service
public class PIDSDJwtClaimsSource extends PIDClaimsSource {


    public PIDSDJwtClaimsSource(FregService fregService, PersonConverterService personConverterService) {
        super(fregService, personConverterService);
    }

    /**
     * Attribute name overrides for PID in SD-JWT VC format.
     */
    @Override
    protected String getAttributeIdentifier(String dataIdentifier) {
        if ("birth_date".equals(dataIdentifier)) {
            return "birthdate";
        }
        if ("birth_place".equals(dataIdentifier)) {
            return "place_of_birth";
        }
        if ("nationality".equals(dataIdentifier)) {
            return "nationalities";
        }
        return super.getAttributeIdentifier(dataIdentifier);
    }

}
