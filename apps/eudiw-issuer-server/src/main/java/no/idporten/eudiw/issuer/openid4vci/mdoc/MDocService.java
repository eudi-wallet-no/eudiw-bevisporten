package no.idporten.eudiw.issuer.openid4vci.mdoc;

import com.nimbusds.jose.jwk.JWK;
import id.walt.mdoc.COSECryptoProviderKeyInfo;
import id.walt.mdoc.SimpleCOSECryptoProvider;
import id.walt.mdoc.dataelement.*;
import id.walt.mdoc.doc.MDoc;
import id.walt.mdoc.doc.MDocBuilder;
import id.walt.mdoc.mso.DeviceKeyInfo;
import id.walt.mdoc.mso.Status;
import kotlinx.datetime.Clock;
import kotlinx.datetime.Instant;
import no.idporten.eudiw.issuer.claimssource.Claim;
import no.idporten.eudiw.issuer.claimssource.DateTimeValue;
import no.idporten.eudiw.issuer.claimssource.FullDateValue;
import no.idporten.eudiw.issuer.claimssource.StringValue;
import no.idporten.eudiw.issuer.crypto.KeyProvider;
import org.cose.java.OneKey;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
            if (entry.getValue() instanceof StringValue(String value)) {
                mDocBuilder.addItemToSign(docType, entry.getPath().getLast(), new StringElement(value));
            } else if (entry.getValue() instanceof DateTimeValue(ZonedDateTime value)) {
                kotlinx.datetime.Instant datetime = Instant.Companion.fromEpochMilliseconds(value.toEpochSecond() * 1000);
                mDocBuilder.addItemToSign(docType, entry.getPath().getLast(), new DateTimeElement(datetime, DEDateTimeMode.tdate));
            } else if (entry.getValue() instanceof FullDateValue(LocalDate value)) {
                kotlinx.datetime.LocalDate kxDate = new kotlinx.datetime.LocalDate(
                        value.getYear(),
                        value.getMonthValue(),
                        value.getDayOfMonth()
                );
                mDocBuilder.addItemToSign(docType, entry.getPath().getLast(), new FullDateElement(kxDate, DEFullDateMode.full_date_str));
            } else {
                mDocBuilder.addItemToSign(docType, entry.getPath().getLast(), new StringElement(String.valueOf(entry.getValue())));
            }

        }
        MDoc mDoc = mDocBuilder.sign(
                new id.walt.mdoc.mso.ValidityInfo(

                        Clock.System.INSTANCE.now(),
                        Clock.System.INSTANCE.now(),
                        new kotlinx.datetime.Instant(java.time.Clock.systemUTC().instant().plus(365, ChronoUnit.DAYS)),
                        new kotlinx.datetime.Instant(java.time.Clock.systemUTC().instant().plus(365, ChronoUnit.DAYS))
                ),
                deviceKeyInfo,
                cryptoProvider,
                ISSUER_KEY_ID,
                (Status) null
        );
        return mDoc;

    }

}
