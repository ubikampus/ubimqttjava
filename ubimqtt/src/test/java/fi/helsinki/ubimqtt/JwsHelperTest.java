package fi.helsinki.ubimqtt;

import com.nimbusds.jose.JOSEException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

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
            assertEquals(null, e);
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
            assertEquals(null, e);
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
            assertEquals(null, e);
        }

        try {
            String home = System.getProperty("user.home");
            String path = home + "/.private/ubimqtt-testing-key-public.pem";

            byte[] encoded = Files.readAllBytes(Paths.get(path));
            publicKey = new String(encoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            assertEquals(null, e);
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

            assertEquals(true, isVerified);

        } catch (Exception e) {
            e.printStackTrace();
            assertEquals(null, e);
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
            assertEquals(null, e);
        }

        try {
            String home = System.getProperty("user.home");
            String path = home + "/.private/ubimqtt-testing-key-public.pem";

            byte[] encoded = Files.readAllBytes(Paths.get(path));
            publicKey = new String(encoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            assertEquals(null, e);
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

            assertEquals(false, isVerified);

        } catch (Exception e) {
            e.printStackTrace();
            assertEquals(null, e);
        }
    }

}
