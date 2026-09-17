package no.idporten.eudiw.issuer.claimssource;

import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WebuildPidDemoClaimsSource extends AbstractAuthorizedClaimsSource {

    @Override
    public CredentialData pull(CredentialIssueContext context) {
        ExtendedCredentialConfiguration configuration = context.credentialConfiguration();
        return new CredentialData(buildDemoClaims(configuration.getFormat().name()));
    }

    private Map<String, Object> buildDemoClaims(String format) {
        Map<String, Object> result = new HashMap<>();

        if ("SD_JWT_VC".equals(format)) {
            result.put("personal_administrative_number", "12345678901");
            result.put("given_name", "Ola");
            result.put("family_name", "Nordmann");
            result.put("birthdate", "1990-02-15");
            result.put("place_of_birth", Map.of("country", "NO"));
            result.put("nationalities", List.of("NO"));
            result.put("expiry_date", "2038-12-31");
            result.put("issuing_authority", "DIGITALISERINGSDIREKTORATET");
            result.put("issuing_country", "NO");
            result.put("resident_address", "Storgata 1");
            result.put("resident_country", "NO");
            result.put("resident_state", "Oslo");
            result.put("resident_city", "Oslo");
            result.put("resident_postal_code", "0150");
            result.put("resident_street", "Storgata");
            result.put("resident_house_number", "1");
            result.put("family_name_birth", "Nordmann");
            result.put("given_name_birth", "Ola");
            result.put("sex", 1);
            result.put("email_address", "ola.nordmann@example.com");
            result.put("mobile_phone_number", "+4799999999");
            result.put("document_number", "A01234567");
            result.put("issuing_jurisdiction", "NO-0301");
            result.put("issuance_date", "2025-01-15");
            result.put("location_status", "https://example.com/statuslists/pid/");
            result.put("trust_anchor", "https://example.com/trustanchors/pid/");
            result.put("attestation_legal_category", "PID");
            result.put("portrait", "data:image/jpeg;base64,AA==");
            return result;
        }

        result.put("personal_administrative_number", "12345678901");
        result.put("given_name", "Ola");
        result.put("family_name", "Nordmann");
        result.put("birth_date", "1990-02-15");
        result.put("place_of_birth", Map.of("country", "NO"));
        result.put("nationality", "NO");
        result.put("expiry_date", "2038-12-31");
        result.put("issuing_authority", "DIGITALISERINGSDIREKTORATET");
        result.put("issuing_country", "NO");
        result.put("resident_address", "Storgata 1");
        result.put("resident_country", "NO");
        result.put("resident_state", "Oslo");
        result.put("resident_city", "Oslo");
        result.put("resident_postal_code", "0150");
        result.put("resident_street", "Storgata");
        result.put("resident_house_number", "1");
        result.put("family_name_birth", "Nordmann");
        result.put("given_name_birth", "Ola");
        result.put("sex", 1);
        result.put("email_address", "ola.nordmann@example.com");
        result.put("mobile_phone_number", "+4799999999");
        result.put("document_number", "A01234567");
        result.put("issuing_jurisdiction", "NO-0301");
        result.put("issuance_date", "2025-01-15");
        result.put("location_status", "https://example.com/statuslists/pid/");
        result.put("trust_anchor", "https://example.com/trustanchors/pid/");
        result.put("attestation_legal_category", "PID");
        result.put("portrait", "data:image/jpeg;base64,AA==");
        return result;
    }
}
