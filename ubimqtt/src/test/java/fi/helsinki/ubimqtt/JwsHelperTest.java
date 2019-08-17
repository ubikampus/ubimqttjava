package fi.helsinki.ubimqtt;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
            String msg = "Hello world";

            String compactResult = JwsHelper.signMessageToCompact(msg, privateKey);
            String jsonResult = JwsHelper.compactToJson(compactResult);
            String newCompactResult = JwsHelper.jsonToCompact(jsonResult);

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
            String msg = "Hello world";

            String compactResult = JwsHelper.signMessageToCompact(msg, privateKey);
            String jsonResult = JwsHelper.compactToJson(compactResult);

            JSONParser parser = new JSONParser();

            JSONObject obj = (JSONObject) parser.parse(jsonResult);
            JSONArray signaturesArray = (JSONArray) obj.get("signatures");
            JSONObject signatureObject = (JSONObject) signaturesArray.get(0);
            signatureObject.put("signature","falsesignature");

            boolean isVerified = JwsHelper.verifySignature(obj.toJSONString(), publicKey);

            assertFalse(isVerified);
        } catch (Exception e) {
            e.printStackTrace();
            assertNull(e);
        }
    }
}
