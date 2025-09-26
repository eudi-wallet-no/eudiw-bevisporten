package no.idporten.eudiw.issuer.openid4vci.mdoc;

import com.nimbusds.jose.jwk.JWK;
import id.walt.mdoc.COSECryptoProviderKeyInfo;
import id.walt.mdoc.SimpleCOSECryptoProvider;
import id.walt.mdoc.dataelement.*;
import id.walt.mdoc.doc.MDoc;
import id.walt.mdoc.doc.MDocBuilder;
import id.walt.mdoc.mso.DeviceKeyInfo;
import kotlinx.datetime.Clock;
import kotlinx.datetime.Instant;
import no.idporten.eudiw.issuer.claimssource.domain.*;
import no.idporten.eudiw.issuer.crypto.KeyProvider;
import org.cose.java.OneKey;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class MDocService {

    private final KeyProvider keyProvider;

    public MDocService(@Qualifier("credentialSigningKeyProvider") KeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    public MDoc issueCredentials(JWK jwk, String docType, List<Claim> claims) throws Exception {
        var ISSUER_KEY_ID = "ISSUER_KEY";
        var DEVICE_KEY_ID = "DEVICE_KEY";
        var READER_KEY_ID = "READER_KEY";

        SimpleCOSECryptoProvider cryptoProvider = new SimpleCOSECryptoProvider(
                List.of(
                        new COSECryptoProviderKeyInfo(
                                ISSUER_KEY_ID,
                                org.cose.java.AlgorithmID.ECDSA_256,
                                keyProvider.publicKey(),
                                keyProvider.privateKey(),
                                List.of(keyProvider.certificate()),
                                List.of(keyProvider.certificate())) // TODO root certs
                ));
        DeviceKeyInfo deviceKeyInfo = new DeviceKeyInfo(DataElement.Companion.fromCBOR(new OneKey(jwk.toECKey().toECPublicKey(), null).AsCBOR().EncodeToBytes()),
                null,
                null);

        MDocBuilder mDocBuilder = new MDocBuilder(docType);
        for (Claim entry : claims) {
            DataElement data = getDataElement(entry.getValue());
            mDocBuilder.addItemToSign(docType, entry.getPath().getLast(), data);
        }
        return mDocBuilder.sign(
                new id.walt.mdoc.mso.ValidityInfo(

                        Clock.System.INSTANCE.now(),
                        Clock.System.INSTANCE.now(),
                        new Instant(java.time.Clock.systemUTC().instant().plus(365, ChronoUnit.DAYS)),
                        new Instant(java.time.Clock.systemUTC().instant().plus(365, ChronoUnit.DAYS))
                ),
                deviceKeyInfo,
                cryptoProvider,
                ISSUER_KEY_ID,
                null
        );
    }

    private static DataElement getDataElement(ClaimValue claimValue) {
        switch (claimValue) {
            case StringValue(String value) -> {
                return new StringElement(value);
            }
            case DateTimeValue(ZonedDateTime value) -> {
                Instant datetime = Instant.Companion.fromEpochMilliseconds(value.toEpochSecond() * 1000);
                return new DateTimeElement(datetime, DEDateTimeMode.tdate);
            }
            case FullDateValue(LocalDate value) -> {
                kotlinx.datetime.LocalDate kxDate = new kotlinx.datetime.LocalDate(
                        value.getYear(),
                        value.getMonthValue(),
                        value.getDayOfMonth()
                );
                return new FullDateElement(kxDate, DEFullDateMode.full_date_str);
            }
            case ListValue(List<ClaimValue> listValue) -> {
                List<DataElement> dataElements = new ArrayList<>();
                for (ClaimValue v : listValue) {
                    dataElements.add(getDataElement(v)); // risky?
                }
                return new ListElement(dataElements);
            }
            case MapValue(Map<String, ClaimValue> m) -> {
                Map<MapKey, DataElement> map = new HashMap<>();
                for (Map.Entry<String, ClaimValue> mapEntry : m.entrySet()) {
                    if (mapEntry.getValue() instanceof StringValue(String value)) {
                        map.put(new MapKey(mapEntry.getKey()), new StringElement(value));
                    } else {
                        throw new IllegalArgumentException("Unsupported map value type: " + mapEntry.getValue().getClass());
                    }
                }
                return new MapElement(map);
            }
            case null, default -> {
                // TODO: throw error instead?
                return new StringElement(String.valueOf(claimValue));
            }
        }

    }
}
