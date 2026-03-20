package no.idporten.eudiw.connector.authoritativesources;

import no.digdir.freg.domain.PersonResource;
import no.idporten.eudiw.connector.authoritativesources.api.Subject;
import no.idporten.eudiw.connector.authoritativesources.krr.model.Kontaktinformasjon;
import no.idporten.eudiw.connector.authoritativesources.krr.model.PersonKrr;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TestData {
    public static String getValidSyntheticPersonIdentifier() {
        return "50917500484";
    }

    public static String getInvalidSyntheticPersonIdentifier() {
        return "5091750048";
    }

    public static String getTestEmailAddress() {
        return "test@default.digdir.no";
    }

    public static String getTestPhoneNumber() {
        return "12345678";
    }

    public static PersonKrr getValidPersonKrr() {
        return new PersonKrr(
                getValidSyntheticPersonIdentifier(),
                "NEI",
                "AKTIV",
                "KAN_VARSLES",
                getValidKontaktinformasjon()
        );
    }

   public static PersonKrr getCustomPersonKrr(String personIdentifier, String reserved, String status, String alertStatus, Kontaktinformasjon contactInfo) {
        return new PersonKrr(
                personIdentifier,
                reserved,
                status,
                alertStatus,
                contactInfo
        );
    }

    public static Kontaktinformasjon getValidKontaktinformasjon() {
        return new Kontaktinformasjon(
                getTestEmailAddress(),
                getTestPhoneNumber()
        );
    }

    public static Subject getValidSubject() {
        return new Subject(getValidSyntheticPersonIdentifier());
    }

    public static PersonResource getAgeYearOldPerson(Integer age) {
        PersonResource p = new PersonResource();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String date = LocalDate.now().minusYears(age).format(formatter);
        p.setFoedselsdato(date);
        return p;
    }


    public static String inntektsApiResponse = """
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
}
