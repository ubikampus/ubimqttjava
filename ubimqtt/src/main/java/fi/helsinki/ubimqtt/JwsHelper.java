package fi.helsinki.ubimqtt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;

public class JwsHelper {

    public static String signMessage( String message, String pemEncodedRSAPrivateKey) throws JOSEException {
        ECKey jwk = (ECKey) JWK.parseFromPEMEncodedObjects(pemEncodedRSAPrivateKey);

        JWSSigner signer = new ECDSASigner(jwk);

        JWSObject jwsObject = new JWSObject(
                new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(jwk.getKeyID()).build(),
                new Payload(message));

        jwsObject.sign(signer);

        String s = jwsObject.serialize();

        return s;
    }

}
