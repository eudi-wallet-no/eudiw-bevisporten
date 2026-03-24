package no.idporten.eudiw.connector.authoritativesources.skatteetaten;


import no.idporten.eudiw.connector.authoritativesources.api.CredentialData;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;
import no.idporten.eudiw.connector.authoritativesources.skatteetaten.domain.Respons;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static no.idporten.eudiw.connector.authoritativesources.TestData.getValidSubject;
import static no.idporten.eudiw.connector.authoritativesources.TestData.inntektsApiResponse;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


@DisplayName("When issuing documents for inntekt")
@ExtendWith(MockitoExtension.class)
public class InntektCredentialDataSourceTest {
    @Mock
    InntektsApiIntegration inntektsApiIntegration;

    @InjectMocks
    InntektCredentialDataSource inntektCredentialDataSource;

    @DisplayName("then data can be retrieved from authoritative source and calculated for each month")
    @Test
    void testRetrieveFromAuthoritativeSource() throws Exception {
        Subject subject = getValidSubject();
        Respons respons = new JsonMapper().readValue(inntektsApiResponse, Respons.class);
        when(inntektsApiIntegration.retrieve(eq(subject.identifier()), any(), any())).thenReturn(respons);
        CredentialData credentialData = inntektCredentialDataSource.retrieveCredentialData(subject);

        assertEquals(1, credentialData.size());

        Map<String, Long> claimMap = (Map<String, Long>)credentialData.get("fastlonn");

        assertAll(
                () -> assertEquals(35050L, claimMap.get("2025-02")),
                () -> assertEquals(35050L, claimMap.get("2025-03")),
                () -> assertEquals(35050L, claimMap.get("2025-04")),
                () -> assertEquals(35050L, claimMap.get("2025-05")),
                () -> assertEquals(38744L, claimMap.get("2025-06")),
                () -> assertEquals(38744L, claimMap.get("2025-07")),
                () -> assertEquals(38744L, claimMap.get("2025-08")),
                () -> assertEquals(38744L, claimMap.get("2025-09")),
                () -> assertEquals(38744L, claimMap.get("2025-10")),
                () -> assertEquals(38744L, claimMap.get("2025-11"))
        );

    }
}
