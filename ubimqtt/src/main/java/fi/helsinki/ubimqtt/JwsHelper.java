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

import java.security.interfaces.ECPrivateKey;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.IOException;
import java.io.StringReader;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;

public class JwsHelper {

    public static String signMessage( String message, String privateKey) throws JOSEException, PEMException, IOException {
        //ECKey jwk = (ECKey) ECKey.parseFromPEMEncodedObjects(pemEncodedRSAPrivateKey);

        // Parse the EC key pair
        //PEMParser pemParser = new PEMParser(new InputStreamReader(new FileInputStream("ec512-key-pair.pem")));
        PEMParser pemParser = new PEMParser(new StringReader(privateKey));

        PEMKeyPair pemKeyPair = (PEMKeyPair)pemParser.readObject();

        // Convert to Java (JCA) format
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        KeyPair keyPair = converter.getKeyPair(pemKeyPair);
        pemParser.close();

        // Get private + public EC key
        ECPrivateKey ecPrivateKey = (ECPrivateKey)keyPair.getPrivate();

        //ECPublicKey publicKey = (ECPublicKey)keyPair.getPublic();

        // Sign test
        JWSObject jwsObject = new JWSObject(new JWSHeader(JWSAlgorithm.ES512), new Payload("message"));
        jwsObject.sign(new ECDSASigner(ecPrivateKey));


        /*
        JWSSigner signer = new ECDSASigner(ecKey);

        JWSObject jwsObject = new JWSObject(
                new JWSHeader.Builder(JWSAlgorithm.RS512).keyID(ecKey.getKeyID()).build(),
                new Payload(message));

        jwsObject.sign(signer);
        */

        String s = jwsObject.serialize();
        return s;
    }

}
