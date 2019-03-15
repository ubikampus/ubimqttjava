package fi.helsinki.ubimqtt;

import com.nimbusds.jose.JOSEException;

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
            String signedResult = JwsHelper.signMessage("Hello world", privateKey);
            System.out.println(signedResult);
        } catch (Exception e) {
            e.printStackTrace();
            assertEquals(null, e);
        }
    }

}
