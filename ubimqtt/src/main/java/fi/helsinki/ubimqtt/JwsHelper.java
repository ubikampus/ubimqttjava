package fi.helsinki.ubimqtt;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.ECDHEncrypter;
import com.nimbusds.jose.crypto.ECDHDecrypter;
import com.nimbusds.jose.util.Base64URL;

import java.security.interfaces.ECPrivateKey;

import org.apache.commons.lang3.RandomStringUtils;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.StringReader;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;

public class JwsHelper {

    public static boolean verifySignature(String json, ECPublicKey publicKey) throws java.text.ParseException, IOException, JOSEException, ParseException {
        return verifySignatureCompact(jsonToCompact(json), publicKey);
    }

    public static boolean verifySignature(String json, String publicKey) throws java.text.ParseException, IOException, JOSEException, ParseException {
        return verifySignatureCompact(jsonToCompact(json), publicKey);
    }

    public static ECPublicKey createEcPublicKey(String publicKey) throws IOException {
        PEMParser pemParser = new PEMParser(new StringReader(publicKey));
        SubjectPublicKeyInfo pemPublicKey = (SubjectPublicKeyInfo)pemParser.readObject();

        // Convert to Java (JCA) format
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        ECPublicKey ecPublicKey = (ECPublicKey)converter.getPublicKey(pemPublicKey);

        pemParser.close();

        return ecPublicKey;
    }

    public static boolean verifySignatureCompact(String compact, String publicKey) throws java.text.ParseException, IOException, JOSEException {
        return verifySignatureCompact(compact, createEcPublicKey(publicKey));
    }

    public static boolean verifySignatureCompact(String compact, ECPublicKey ecPublicKey) throws java.text.ParseException, IOException, JOSEException {

        String[] parts = compact.split("\\.");

        Base64URL header = new Base64URL(parts[0]);
        Base64URL payload = new Base64URL(parts[1]);
        Base64URL signature = new Base64URL(parts[2]);

        JWSObject jwsObject = new JWSObject(header, payload, signature);
        JWSVerifier verifier = new ECDSAVerifier(ecPublicKey);

        return jwsObject.verify(verifier);
    }

    public static String signMessage( String message, String privateKey) throws JOSEException, ParseException, IOException {
        return compactToJson(signMessageToCompact(message, privateKey));
    }

    public static String signMessageToCompact( String message, String privateKey) throws JOSEException, IOException {
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

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES512).
                customParam("timestamp", System.currentTimeMillis()).
                customParam("messageid", RandomStringUtils.randomAlphanumeric(12)).
                build();

        JWSObject jwsObject = new JWSObject(header, new Payload(message));
        jwsObject.sign(new ECDSASigner(ecPrivateKey));


        /*
        JWSSigner signer = new ECDSASigner(ecKey);

        JWSObject jwsObject = new JWSObject(
                new JWSHeader.Builder(JWSAlgorithm.RS512).keyID(ecKey.getKeyID()).build(),
                new Payload(message));

        jwsObject.sign(signer);
        */

        return jwsObject.serialize();
    }

    public static String compactToJson(String compact) throws ParseException{
        String[] parts = compact.split("\\.");

        String header = new Base64URL(parts[0]).decodeToString();
        String payload = new Base64URL(parts[1]).decodeToString();
        String signature = parts[2];

        System.out.println("header: " +header);
        System.out.println("payload: " +payload);
        System.out.println("signature: " +signature);

        JSONObject obj = new JSONObject();

        JSONObject signatureObject = new JSONObject();

        JSONParser parser = new JSONParser();
        JSONObject headerObj = (JSONObject) parser.parse(header);

        signatureObject.put("protected", headerObj);
        signatureObject.put("signature", signature);

        JSONArray signaturesArray = new JSONArray();
        signaturesArray.add(signatureObject);

        obj.put("payload", payload);
        obj.put("signatures", signaturesArray);

        return obj.toJSONString();
    }


  public static String jsonToCompact(String json) throws ParseException {
      JSONParser parser = new JSONParser();
      JSONObject obj = (JSONObject) parser.parse(json);

      String payload = (String)obj.get("payload");

      JSONArray signaturesArray = (JSONArray)obj.get("signatures");
      JSONObject signatureObject = (JSONObject)signaturesArray.get(0);

      String header = ((JSONObject)signatureObject.get("protected")).toJSONString();
      String signature = (String)signatureObject.get("signature");


      return Base64URL.encode(header)+"."+Base64URL.encode(payload)+"."+signature;
    }

    public static String encryptMessage(String message, ECPublicKey ecPublicKey) throws JOSEException {
        JWEAlgorithm alg = JWEAlgorithm.ECDH_ES;
        EncryptionMethod enc = EncryptionMethod.A128CBC_HS256;

        // Encrypt the JWE with the EC public key
        JWEObject jwe = new JWEObject(new JWEHeader(alg, enc), new Payload(message));
        jwe.encrypt(new ECDHEncrypter(ecPublicKey));
        return jwe.serialize();
    }

    public static String encryptMessage(String message, String publicKey) throws IOException, JOSEException {
        return encryptMessage(message, createEcPublicKey(publicKey));
    }

    public static String decryptMessage(String message, String privateKey) throws JOSEException, IOException, java.text.ParseException {
        // Parse the EC key pair
        PEMParser pemParser = new PEMParser(new StringReader(privateKey));
        PEMKeyPair pemKeyPair = (PEMKeyPair)pemParser.readObject();

        // Convert to Java (JCA) format
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        KeyPair keyPair = converter.getKeyPair(pemKeyPair);
        pemParser.close();

        // Get private EC key
        ECPrivateKey ecPrivateKey = (ECPrivateKey)keyPair.getPrivate();

        // Decrypt the JWE with the EC private key
        JWEObject jwe = JWEObject.parse(message);
        jwe.decrypt(new ECDHDecrypter(ecPrivateKey));

        return jwe.getPayload().toString();
    }
}
