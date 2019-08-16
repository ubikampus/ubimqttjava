package fi.helsinki.ubimqtt;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDHDecrypter;
import com.nimbusds.jose.crypto.ECDHEncrypter;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import static org.junit.Assert.*;

public class JwsHelperTest {

    @Test
    public void testJwsHelper_CanSign() {
        java.security.Security.addProvider(com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton.getInstance());

        String privateKey = "";
        try {
            String home = System.getProperty("user.home");
            String path = home + "/.private/ubimqtt-testing-key.pem";

            byte[] encoded = Files.readAllBytes(Paths.get(path));
            privateKey = new String(encoded, StandardCharsets.UTF_8);

        } catch (Exception e) {
            assertNull(e);
        }

        try {
            String compactResult = JwsHelper.signMessageToCompact("Hello world", privateKey);
            System.out.println(compactResult);

            String jsonResult = JwsHelper.compactToJson(compactResult);
            System.out.println(jsonResult);

            String newCompactResult = JwsHelper.jsonToCompact(jsonResult);
            System.out.println(newCompactResult);

            assertEquals(compactResult, newCompactResult);

        } catch (Exception e) {
            e.printStackTrace();
            assertNull(e);
        }
    }


    @Test
    public void testJwsHelper_CanSignAndVerify() {
        java.security.Security.addProvider(com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton.getInstance());

        String privateKey = "";
        String publicKey = "";
        try {
            String home = System.getProperty("user.home");
            String path = home + "/.private/ubimqtt-testing-key.pem";

            byte[] encoded = Files.readAllBytes(Paths.get(path));
            privateKey = new String(encoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            assertNull(e);
        }

        try {
            String home = System.getProperty("user.home");
            String path = home + "/.private/ubimqtt-testing-key-public.pem";

            byte[] encoded = Files.readAllBytes(Paths.get(path));
            publicKey = new String(encoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            assertNull(e);
        }

        try {
            String compactResult = JwsHelper.signMessageToCompact("Hello world", privateKey);
            System.out.println(compactResult);

            String jsonResult = JwsHelper.compactToJson(compactResult);
            System.out.println(jsonResult);

            String newCompactResult = JwsHelper.jsonToCompact(jsonResult);
            System.out.println(newCompactResult);

            assertEquals(compactResult, newCompactResult);

            boolean isVerified = JwsHelper.verifySignatureCompact(newCompactResult, publicKey);

            assertTrue(isVerified);

        } catch (Exception e) {
            e.printStackTrace();
            assertNull(e);
        }
    }

    @Test
    public void testJwsHelper_CanDetectFalseSignature() {
        java.security.Security.addProvider(com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton.getInstance());

        String privateKey = "";
        String publicKey = "";
        try {
            String home = System.getProperty("user.home");
            String path = home + "/.private/ubimqtt-testing-key.pem";

            byte[] encoded = Files.readAllBytes(Paths.get(path));
            privateKey = new String(encoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            assertNull(e);
        }

        try {
            String home = System.getProperty("user.home");
            String path = home + "/.private/ubimqtt-testing-key-public.pem";

            byte[] encoded = Files.readAllBytes(Paths.get(path));
            publicKey = new String(encoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            assertNull(e);
        }

        try {
            String compactResult = JwsHelper.signMessageToCompact("Hello world", privateKey);
            System.out.println(compactResult);

            String jsonResult = JwsHelper.compactToJson(compactResult);
            System.out.println(jsonResult);

            JSONParser parser = new JSONParser();

            JSONObject obj = (JSONObject) parser.parse(jsonResult);
            JSONArray signaturesArray = (JSONArray) obj.get("signatures");
            JSONObject signatureObject = (JSONObject)signaturesArray.get(0);
            signatureObject.put("signature","falsesignature");


            boolean isVerified = JwsHelper.verifySignature(obj.toJSONString(), publicKey);

            assertFalse(isVerified);

        } catch (Exception e) {
            e.printStackTrace();
            assertNull(e);
        }
    }

    @Test
    public void testJwsHelper_canEncryptMessage() {
        java.security.Security.addProvider(com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton.getInstance());

        String privateKey = "";
        String publicKey = "";
        try {
            String home = System.getProperty("user.home");
            String path = home + "/.private/ubimqtt-testing-key.pem";

            byte[] encoded = Files.readAllBytes(Paths.get(path));
            privateKey = new String(encoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            assertNull(e);
        }

        try {
            String home = System.getProperty("user.home");
            String path = home + "/.private/ubimqtt-testing-key-public.pem";

            byte[] encoded = Files.readAllBytes(Paths.get(path));
            publicKey = new String(encoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            assertNull(e);
        }

        try {
            String msg = "Hello World!";

            // Encrypt test
            String encryptMessage = JwsHelper.encryptMessage(msg, publicKey);

            // Parse for check
            PEMParser pemParser = new PEMParser(new StringReader(privateKey));
            PEMKeyPair pemKeyPair = (PEMKeyPair)pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            KeyPair keyPair = converter.getKeyPair(pemKeyPair);
            pemParser.close();

            ECPrivateKey ecPrivateKey = (ECPrivateKey)keyPair.getPrivate();
            JWEObject jwe = JWEObject.parse(encryptMessage);
            jwe.decrypt(new ECDHDecrypter(ecPrivateKey));

            // Check
            assertEquals(msg, jwe.getPayload().toString());
        } catch (Exception e) {
            e.printStackTrace();
            assertNull(e);
        }
    }

    @Test
    public void testJwsHelper_canDecryptMessage() {
        java.security.Security.addProvider(com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton.getInstance());

        String privateKey = "";
        String publicKey = "";
        try {
            String home = System.getProperty("user.home");
            String path = home + "/.private/ubimqtt-testing-key.pem";

            byte[] encoded = Files.readAllBytes(Paths.get(path));
            privateKey = new String(encoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            assertNull(e);
        }

        try {
            String home = System.getProperty("user.home");
            String path = home + "/.private/ubimqtt-testing-key-public.pem";

            byte[] encoded = Files.readAllBytes(Paths.get(path));
            publicKey = new String(encoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            assertNull(e);
        }

        try {
            String msg = "Hello World!";

            // Create encrypted message
            PEMParser pemParser = new PEMParser(new StringReader(publicKey));
            SubjectPublicKeyInfo pemPublicKey = (SubjectPublicKeyInfo)pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            ECPublicKey ecPublicKey = (ECPublicKey)converter.getPublicKey(pemPublicKey);
            pemParser.close();

            JWEAlgorithm alg = JWEAlgorithm.ECDH_ES;
            EncryptionMethod enc = EncryptionMethod.A128CBC_HS256;
            JWEObject jwe = new JWEObject(new JWEHeader(alg, enc), new Payload(msg));
            jwe.encrypt(new ECDHEncrypter(ecPublicKey));
            String serialize = jwe.serialize();

            // Decrypt test
            String decryptMessage = JwsHelper.decryptMessage(serialize, privateKey);

            // Check
            assertEquals(msg, decryptMessage);
        } catch (Exception e) {
            e.printStackTrace();
            assertNull(e);
        }
    }

    @Test
    public void testJwsHelper_canDecryptOwnEncrypting() {
        java.security.Security.addProvider(com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton.getInstance());

        String privateKey = "";
        String publicKey = "";
        try {
            String home = System.getProperty("user.home");
            String path = home + "/.private/ubimqtt-testing-key.pem";

            byte[] encoded = Files.readAllBytes(Paths.get(path));
            privateKey = new String(encoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            assertNull(e);
        }

        try {
            String home = System.getProperty("user.home");
            String path = home + "/.private/ubimqtt-testing-key-public.pem";

            byte[] encoded = Files.readAllBytes(Paths.get(path));
            publicKey = new String(encoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            assertNull(e);
        }

        try {
            String msg = "Hello World!";
            String encryptMessage = JwsHelper.encryptMessage(msg, publicKey);
            String decryptMessage = JwsHelper.decryptMessage(encryptMessage, privateKey);

            assertEquals(msg, decryptMessage);
            assertNotEquals(msg, encryptMessage);
            assertNotEquals(decryptMessage, encryptMessage);
        } catch (Exception e) {
            e.printStackTrace();
            assertNull(e);
        }
    }
}
