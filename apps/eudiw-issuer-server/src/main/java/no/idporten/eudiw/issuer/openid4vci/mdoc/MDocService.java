package no.idporten.eudiw.issuer.openid4vci.mdoc;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import id.walt.mdoc.COSECryptoProviderKeyInfo;
import id.walt.mdoc.SimpleCOSECryptoProvider;
import id.walt.mdoc.dataelement.DataElement;
import id.walt.mdoc.dataelement.StringElement;
import id.walt.mdoc.doc.MDoc;
import id.walt.mdoc.doc.MDocBuilder;
import id.walt.mdoc.mso.DeviceKeyInfo;
import id.walt.mdoc.mso.Status;
import kotlinx.datetime.Clock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eudiw.issuer.claimssource.Claim;
import no.idporten.eudiw.issuer.crypto.KeyProvider;
import org.cose.java.OneKey;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MDocService {

    private final KeyProvider keyProvider;

    public MDoc issueCredentials(JWK jwk, String docType, List<Claim> claims) throws Exception {

        // TODO erstatte med proof, egen sak
        jwk = new ECKeyGenerator(Curve.P_256).generate();

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
//                        ,
                ));
        DeviceKeyInfo deviceKeyInfo = new DeviceKeyInfo(DataElement.Companion.fromCBOR(new OneKey(jwk.toECKey().toECPublicKey(), null).AsCBOR().EncodeToBytes()),
                null,
                null);

        MDocBuilder mDocBuilder = new MDocBuilder(docType);
        for (Claim entry : claims) {
            if (entry.getValue() instanceof String) {
                mDocBuilder.addItemToSign(docType, entry.getPath().getLast(), new StringElement(entry.getValue()));
                // TODO datatyoer egen sak
//            } else if ("number".equals(entry.getType())) {
//                mDocBuilder.addItemToSign(docType, entry.getKey(), new NumberElement((Number) entry.getValue()));
//            } else if ("boolean".equals(entry.getType())) {
//                mDocBuilder.addItemToSign(docType, entry.getKey(), new BooleanElement((Boolean) entry.getValue()));
//            } else if (entry.getValue() instanceof Collection<?>) {
//                for (Object v : (Collection<?>) entry.getValue()) {
//                    mDocBuilder.addItemToSign(docType, entry.getKey(), new StringElement(String.valueOf(v)));
//                }
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
