package no.idporten.eudiw.issuer.claimssource.byob.domain;

import no.idporten.eudiw.issuer.credentials.types.DocumentMetadata;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DynamicCredentialMetadataTest {

    @Test
    void convertToDocumentMetadata() {
        DynamicCredentialMetadata metadata = new DynamicCredentialMetadata(List.of(new DocumentMetadata.Display("no","my-name")), List.of(new DynamicClaimMetadata("claim1", List.of(new DocumentMetadata.Display("en","claim-name")), true, null)));
        var convertedMetadata = metadata.convertToDocumentMetadata();
        assertNotNull(convertedMetadata);
        assertEquals(metadata.display().size(), convertedMetadata.displays().size());
        for(DocumentMetadata.Display d : metadata.display()){
           assertNotNull(convertedMetadata.displays().stream().filter(convertDisplay -> d.locale().equals(convertDisplay.locale())).findFirst(), "Local does not match converted");
           assertNotNull(convertedMetadata.displays().stream().filter(convertDisplay -> d.name().equals(convertDisplay.name())).findFirst(), "Name does not match converted");
        }

        assertEquals(metadata.claims().size(), convertedMetadata.claims().size());
        for(DynamicClaimMetadata c : metadata.claims()){
            var convertedClaim = convertedMetadata.claims().stream().filter(convertClaim -> c.path().equals(convertClaim.name())).findFirst().orElse(null);
            assertNotNull(convertedClaim, "Claim path does not match converted");

            for(DocumentMetadata.Display d : c.display()){
                assertNotNull(convertedClaim.getDisplayName(d.locale()), "Locale in claim display not found in converted");
                assertEquals(d.name(), convertedClaim.getDisplayName(d.locale()), "Name in claim display does not match converted");
            }
        }
    }

    @Test
    void convertToDocumentMetadataWithNullClaimsAndDisplay() {
        DynamicCredentialMetadata metadata = new DynamicCredentialMetadata(null, null);
        var convertedMetadata = metadata.convertToDocumentMetadata();
        assertNotNull(convertedMetadata);
        assertNull(convertedMetadata.displays());
        assertNull(convertedMetadata.claims());
    }

    @Test
    void convertToDocumentMetadataWithEmptyClaimsAndDisplay() {
        DynamicCredentialMetadata metadata = new DynamicCredentialMetadata(List.of(), List.of());
        var convertedMetadata = metadata.convertToDocumentMetadata();
        assertNotNull(convertedMetadata);
        assertNotNull(convertedMetadata.displays());
        assertNotNull(convertedMetadata.claims());
        assertEquals(metadata.display().size(), convertedMetadata.displays().size());
        assertEquals(metadata.claims().size(), convertedMetadata.claims().size());
    }

    @Test
    void convertToDocumentMetadataWithNullClaims() {
        DynamicCredentialMetadata metadata = new DynamicCredentialMetadata(List.of(), null);
        var convertedMetadata = metadata.convertToDocumentMetadata();
        assertNotNull(convertedMetadata);
        assertNotNull(convertedMetadata.displays());
        assertEquals(metadata.display().size(), convertedMetadata.displays().size());
        assertNull(convertedMetadata.claims());

    }
}
