package no.idporten.eudiw.bevisgenerator.integration.verifierservice;

import no.idporten.eudiw.bevisgenerator.integration.byobservice.model.Display;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.credentialdefinitionmodel.ClaimMetadata;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.credentialdefinitionmodel.CredentialConfiguration;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.credentialdefinitionmodel.CredentialConfigurationMetadata;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.credentialdefinitionmodel.CredentialIssuerMetadata;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.CredentialDefinitionDisplayData;
import no.idporten.eudiw.bevisgenerator.integration.verifierservice.model.SelectableClaim;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class DCQLServiceImpl implements DCQLService {

    private static final Pattern NON_ALLOWED = Pattern.compile("[^A-Za-z0-9_-]+");

    @Override
    public List<CredentialDefinitionDisplayData> createCredentialDefinitionDisplayData(List<CredentialIssuerMetadata> credentialIssuerMetadata) {
        List<CredentialDefinitionDisplayData> displayDataList = new ArrayList<>();

        if (credentialIssuerMetadata == null || credentialIssuerMetadata.isEmpty()) {
            return displayDataList;
        }

        for (CredentialIssuerMetadata metadata : credentialIssuerMetadata) {

            if (metadata == null) {
                continue;
            }

            for (String key : metadata.credentialConfigurationsSupported().keySet()) {
                CredentialConfiguration config = metadata.credentialConfigurationsSupported().get(key);

                if (config == null) {
                    continue;
                }

                displayDataList.add(buildCredentialDefinitionDisplayData(key, config, metadata.credentialIssuer()));
            }
        }

        return displayDataList;
    }

    @Override
    public Map<String, Object> buildDcqlMap(CredentialDefinitionDisplayData credentialDefinitionDisplayData, List<String> selectedClaimPaths) {
        return Map.of(
                "credentials", List.of(Map.of(
                        "id", normalizeDcqlId(credentialDefinitionDisplayData.id()),
                        "format", credentialDefinitionDisplayData.format(),
                        "meta", credentialDefinitionDisplayData.meta(),
                        "claims", getClaimsWithFullPath(credentialDefinitionDisplayData, selectedClaimPaths)
                ))
        );
    }

    private static CredentialDefinitionDisplayData buildCredentialDefinitionDisplayData(
            String key,
            CredentialConfiguration config,
            String issuer
    ) {
        CredentialConfigurationMetadata credentialMetadata = config.credentialMetadata();

        Display display = credentialMetadata != null && credentialMetadata.display() != null
                ? credentialMetadata.display().stream().findFirst().orElse(null)
                : null;
        List<ClaimMetadata> claimMetadata = credentialMetadata != null && credentialMetadata.claims() != null
                ? credentialMetadata.claims()
                : List.of();

        List<SelectableClaim> claims = getSelectableClaims(claimMetadata);

        Map<String, Object> meta = formatCredentialConfigurationMetadata(config);

        return new CredentialDefinitionDisplayData(
                key,
                display != null ? display.name() : "No display name found",
                issuer,
                config.format(),
                meta,
                claims
        );
    }

    private static @NonNull List<Map<String, List<String>>> getClaimsWithFullPath(CredentialDefinitionDisplayData credentialDefinitionDisplayData, List<String> selectedClaimPaths) {
        Set<String> selectedPaths = selectedClaimPaths == null
                ? Set.of()
                : new HashSet<>(selectedClaimPaths);
        return credentialDefinitionDisplayData.claims().stream()
                .filter(claim -> selectedPaths.contains(String.join(".", claim.path())))
                .map(claim -> Map.of("path", claim.path()))
                .toList();
    }

    static String normalizeDcqlId(String key) {
        String base = key == null ? "" : NON_ALLOWED.matcher(key).replaceAll("-");
        base = base + "-" + UUID.randomUUID();
        base = base.replaceAll("^-+|-+$", "");   // trim leading/trailing '-'
        return base.replaceAll("-{2,}", "-");    // collapse repeated '-'
    }

    private static @NonNull List<SelectableClaim> getSelectableClaims(List<ClaimMetadata> claimMetadata) {
        List<SelectableClaim> claims = new ArrayList<>();
        for (ClaimMetadata claim : claimMetadata) {
            claims.add(new SelectableClaim(
                    claim.display() != null
                            ? claim.display().stream().findFirst().map(Display::name).orElse("No display name found")
                            : "No display name found",
                    claim.path() != null ? claim.path() : List.of())
            );
        }
        return claims;
    }

    private static Map<String, Object> formatCredentialConfigurationMetadata(CredentialConfiguration config) {
        Map<String, Object> meta = new HashMap<>();
        if ("dc+sd-jwt".equals(config.format()) && config.vct() != null) {
            meta.put("vct_values", List.of(config.vct()));
        } else if (config.doctype() != null) {
            meta.put("doctype_value", config.doctype());
        }

        return meta;
    }
}
