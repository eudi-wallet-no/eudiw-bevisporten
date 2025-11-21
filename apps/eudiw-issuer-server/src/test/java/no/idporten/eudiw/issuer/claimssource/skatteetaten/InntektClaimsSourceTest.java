package no.idporten.eudiw.issuer.claimssource.skatteetaten;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.issuer.claimssource.domain.Claim;
import no.idporten.eudiw.issuer.claimssource.domain.MapValue;
import no.idporten.eudiw.issuer.claimssource.skatteetaten.domain.Respons;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


@DisplayName("When issuing documents for inntekt")
@ActiveProfiles("junit")
@SpringBootTest
public class InntektClaimsSourceTest {

    @Autowired
    InntektClaimsSource claimsSource;

    @MockitoBean
    InntektsApiIntegration inntektsApiIntegration;

    @DisplayName("then data can be retrieved from authoritative source and calculated for each month")
    @Test
    void testRetrieveFromAuthoritativeSource() throws Exception {
        String personIdentifier = "12345678901";
        JWT accessToken = new PlainJWT(new JWTClaimsSet.Builder().claim("sub", personIdentifier).build());
        String response = """
                {
                  "oppgaveInntektsmottaker": [
                    {
                      "opplysningspliktigId": "311169254",
                      "virksomhetId": "311169254",
                      "kalendermaaned": "2025-01",
                      "inntektsmottaker": {
                        "personidentifikator": "20848997790"
                      },
                      "mottaksinformasjon": {
                        "foersteMottakstidspunkt": "2025-01-20T10:38:58.976+01:00"
                      }
                    },
                    {
                      "opplysningspliktigId": "312576511",
                      "virksomhetId": "311513176",
                      "kalendermaaned": "2025-02",
                      "inntektsmottaker": {
                        "personidentifikator": "20848997790"
                      },
                      "inntekt": [
                        {
                          "beloep": 35050.0,
                          "loennsinntekt": {
                            "beskrivelse": "fastloenn"
                          }
                        }
                      ],
                      "mottaksinformasjon": {}
                    },
                    {
                      "opplysningspliktigId": "312576511",
                      "virksomhetId": "311513176",
                      "kalendermaaned": "2025-03",
                      "inntektsmottaker": {
                        "personidentifikator": "20848997790"
                      },
                      "inntekt": [
                        {
                          "beloep": 35050.0,
                          "loennsinntekt": {
                            "beskrivelse": "fastloenn"
                          }
                        }
                      ],
                      "mottaksinformasjon": {}
                    },
                    {
                      "opplysningspliktigId": "312576511",
                      "virksomhetId": "311513176",
                      "kalendermaaned": "2025-04",
                      "inntektsmottaker": {
                        "personidentifikator": "20848997790"
                      },
                      "inntekt": [
                        {
                          "beloep": 35050.0,
                          "loennsinntekt": {
                            "beskrivelse": "fastloenn"
                          }
                        }
                      ],
                      "mottaksinformasjon": {}
                    },
                    {
                      "opplysningspliktigId": "312576511",
                      "virksomhetId": "311513176",
                      "kalendermaaned": "2025-05",
                      "inntektsmottaker": {
                        "personidentifikator": "20848997790"
                      },
                      "inntekt": [
                        {
                          "beloep": 35050.0,
                          "loennsinntekt": {
                            "beskrivelse": "fastloenn"
                          }
                        }
                      ],
                      "mottaksinformasjon": {
                        "foersteMottakstidspunkt": "2025-05-14T11:20:23.968+02:00"
                      }
                    },
                    {
                      "opplysningspliktigId": "312576511",
                      "virksomhetId": "311513176",
                      "kalendermaaned": "2025-06",
                      "inntektsmottaker": {
                        "personidentifikator": "20848997790"
                      },
                      "inntekt": [
                        {
                          "beloep": 38744.0,
                          "loennsinntekt": {
                            "beskrivelse": "fastloenn"
                          }
                        }
                      ],
                      "mottaksinformasjon": {
                        "foersteMottakstidspunkt": "2025-10-07T07:59:21.399+02:00"
                      }
                    },
                    {
                      "opplysningspliktigId": "312576511",
                      "virksomhetId": "311513176",
                      "kalendermaaned": "2025-07",
                      "inntektsmottaker": {
                        "personidentifikator": "20848997790"
                      },
                      "inntekt": [
                        {
                          "beloep": 38744.0,
                          "loennsinntekt": {
                            "beskrivelse": "fastloenn"
                          }
                        }
                      ],
                      "mottaksinformasjon": {
                        "foersteMottakstidspunkt": "2025-10-07T07:59:17.08+02:00"
                      }
                    },
                    {
                      "opplysningspliktigId": "312576511",
                      "virksomhetId": "311513176",
                      "kalendermaaned": "2025-08",
                      "inntektsmottaker": {
                        "personidentifikator": "20848997790"
                      },
                      "inntekt": [
                        {
                          "beloep": 38744.0,
                          "loennsinntekt": {
                            "beskrivelse": "fastloenn"
                          }
                        }
                      ],
                      "mottaksinformasjon": {
                        "foersteMottakstidspunkt": "2025-10-07T07:59:14.586+02:00"
                      }
                    },
                    {
                      "opplysningspliktigId": "312576511",
                      "virksomhetId": "311513176",
                      "kalendermaaned": "2025-09",
                      "inntektsmottaker": {
                        "personidentifikator": "20848997790"
                      },
                      "inntekt": [
                        {
                          "beloep": 38744.0,
                          "loennsinntekt": {
                            "beskrivelse": "fastloenn"
                          }
                        }
                      ],
                      "mottaksinformasjon": {
                        "foersteMottakstidspunkt": "2025-10-07T07:59:14.28+02:00"
                      }
                    },
                    {
                      "opplysningspliktigId": "312576511",
                      "virksomhetId": "311513176",
                      "kalendermaaned": "2025-10",
                      "inntektsmottaker": {
                        "personidentifikator": "20848997790"
                      },
                      "inntekt": [
                        {
                          "beloep": 38744.0,
                          "loennsinntekt": {
                            "beskrivelse": "fastloenn"
                          }
                        }
                      ],
                      "mottaksinformasjon": {
                        "foersteMottakstidspunkt": "2025-10-07T07:59:12.051+02:00"
                      }
                    },
                    {
                      "opplysningspliktigId": "312576511",
                      "virksomhetId": "311513176",
                      "kalendermaaned": "2025-11",
                      "inntektsmottaker": {
                        "personidentifikator": "20848997790"
                      },
                      "inntekt": [
                        {
                          "beloep": 38744.0,
                          "loennsinntekt": {
                            "beskrivelse": "fastloenn"
                          }
                        }
                      ],
                      "mottaksinformasjon": {
                        "foersteMottakstidspunkt": "2025-10-07T07:59:11.742+02:00"
                      }
                    }
                  ]
                }""";
        Respons respons = new ObjectMapper().readValue(response, Respons.class);
        when(inntektsApiIntegration.retrieve(eq(personIdentifier))).thenReturn(respons);
        List<Claim> claims = claimsSource.retrieveClaims(accessToken);
        assertAll(
                () -> assertEquals(1, claims.size()),
                () -> assertTrue(claims.getFirst().getValue() instanceof MapValue)
        );
        MapValue claimMap = (MapValue) claims.getFirst().getValue();
        assertAll(
                () -> assertEquals(35050L, claimMap.value().get("2025-02").value()),
                () -> assertEquals(35050L, claimMap.value().get("2025-03").value()),
                () -> assertEquals(35050L, claimMap.value().get("2025-04").value()),
                () -> assertEquals(35050L, claimMap.value().get("2025-05").value()),
                () -> assertEquals(38744L, claimMap.value().get("2025-06").value()),
                () -> assertEquals(38744L, claimMap.value().get("2025-07").value()),
                () -> assertEquals(38744L, claimMap.value().get("2025-08").value()),
                () -> assertEquals(38744L, claimMap.value().get("2025-09").value()),
                () -> assertEquals(38744L, claimMap.value().get("2025-10").value()),
                () -> assertEquals(38744L, claimMap.value().get("2025-11").value())
        );

    }
}
