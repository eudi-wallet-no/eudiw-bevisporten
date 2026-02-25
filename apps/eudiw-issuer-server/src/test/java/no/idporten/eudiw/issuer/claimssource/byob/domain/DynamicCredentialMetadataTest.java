package no.idporten.eudiw.issuer.claimssource.byob.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DynamicCredentialMetadataTest {

    @Test
    void toExtendedCredentialMetadataWithNullClaimsAndDisplay() {
        DynamicCredentialMetadata metadata = new DynamicCredentialMetadata(null, null);
        var convertedMetadata = metadata.toExtendedCredentialMetadata(mock(DynamicCredentialConfiguration.class));
        assertNotNull(convertedMetadata);
        assertNull(convertedMetadata.display());
        assertNull(convertedMetadata.claims());
    }

    @Test
    void toExtendedCredentialMetadataWithEmptyClaimsAndDisplay() {
        DynamicCredentialMetadata metadata = new DynamicCredentialMetadata(List.of(), List.of());
        var convertedMetadata = metadata.toExtendedCredentialMetadata(mock(DynamicCredentialConfiguration.class));
        assertNotNull(convertedMetadata);
        assertNotNull(convertedMetadata.display());
        assertNotNull(convertedMetadata.claims());
        assertEquals(metadata.display().size(), convertedMetadata.display().size());
        assertEquals(metadata.claims().size(), convertedMetadata.claims().size());
    }

    @Test
    void toExtendedCredentialMetadataWithNullClaims() {
        DynamicCredentialMetadata metadata = new DynamicCredentialMetadata(List.of(), null);
        var convertedMetadata = metadata.toExtendedCredentialMetadata(mock(DynamicCredentialConfiguration.class));
        assertNotNull(convertedMetadata);
        assertNotNull(convertedMetadata.display());
        assertEquals(metadata.display().size(), convertedMetadata.display().size());
        assertNull(convertedMetadata.claims());

    }
}
