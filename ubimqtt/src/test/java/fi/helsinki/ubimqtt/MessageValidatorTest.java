package fi.helsinki.ubimqtt;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class MessageValidatorTest {

    private String signMessage(String message) {
        java.security.Security.addProvider(com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton.getInstance());

        String privateKey = "";
        String jsonResult = null;

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

            jsonResult = JwsHelper.compactToJson(compactResult);
            System.out.println(jsonResult);

            String newCompactResult = JwsHelper.jsonToCompact(jsonResult);
            System.out.println(newCompactResult);

            assertEquals(compactResult, newCompactResult);

        } catch (Exception e) {
            e.printStackTrace();
            assertEquals(null, e);
        }
        return jsonResult;
    }

    @Test
    public void testMessageValidator_CanDetectReplayedMessage() {
        try {
            String signedMessage = this.signMessage("Testjee");

            MessageValidator messageValidator = new MessageValidator(60);

            String publicKey = null;

            try {
                String home = System.getProperty("user.home");
                String path = home + "/.private/ubimqtt-testing-key-public.pem";

                byte[] encoded = Files.readAllBytes(Paths.get(path));
                publicKey = new String(encoded, StandardCharsets.UTF_8);
            } catch (Exception e) {
                assertEquals(null, e);
            }

            System.out.println("Trying to validate message");

            boolean firstResult = messageValidator.validateMessage(signedMessage, JwsHelper.createEcPublicKey(publicKey));
            assertEquals(true, firstResult);

            boolean secondResult = messageValidator.validateMessage(signedMessage, JwsHelper.createEcPublicKey(publicKey));
            assertEquals(false, secondResult);

        } catch (Exception e) {
            e.printStackTrace();
            assertEquals(null, e);
        }
    }

    @Test
    public void testMessageValidator_CanDetectTooOldMessage() {
        try {
            String signedMessage = this.signMessage("Testjee");

            MessageValidator messageValidator = new MessageValidator(1);

            String publicKey = null;

            try {
                String home = System.getProperty("user.home");
                String path = home + "/.private/ubimqtt-testing-key-public.pem";

                byte[] encoded = Files.readAllBytes(Paths.get(path));
                publicKey = new String(encoded, StandardCharsets.UTF_8);
            } catch (Exception e) {
                assertEquals(null, e);
            }

            System.out.println("Trying to validate a message that is one second too old, sleeping 2 seconds");

            Thread.sleep(5000);

            boolean firstResult = messageValidator.validateMessage(signedMessage, JwsHelper.createEcPublicKey(publicKey));
            assertEquals(false, firstResult);

        } catch (Exception e) {
            e.printStackTrace();
            assertEquals(null, e);
        }
    }
}

