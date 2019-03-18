package fi.helsinki.ubimqtt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.util.Base64URL;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.security.interfaces.ECPublicKey;

public class MessageValidator {

    private int maximumReplayBufferSize;
    private ReplayDetector replayDetector;

    public MessageValidator(int maximumReplayBufferSize) {
        this.maximumReplayBufferSize = maximumReplayBufferSize;
        this.replayDetector = new ReplayDetector(maximumReplayBufferSize);
    }

    public boolean validateMessage(String message, ECPublicKey ecPublicKey) throws ParseException, JOSEException, java.text.ParseException, IOException {
        JSONParser parser = new JSONParser();
        JSONObject obj = (JSONObject) parser.parse(message);

        String payload = (String)obj.get("payload");

        JSONArray signaturesArray = (JSONArray)obj.get("signatures");
        JSONObject signatureObject = (JSONObject)signaturesArray.get(0);
        JSONObject headerObj = (JSONObject)signatureObject.get("protected");

        String signature = (String)signatureObject.get("signature");

        String compact = Base64URL.encode(headerObj.toString())+"."+Base64URL.encode(payload)+"."+signature;

        boolean isSignatureCorrect = JwsHelper.verifySignatureCompact(compact, ecPublicKey);

        if (!isSignatureCorrect)
            return false;

        long timestamp = Long.parseLong((String)headerObj.get("timestamp"));
        String nonce = (String)headerObj.get("nonce");

        return replayDetector.isValid(timestamp, nonce);
    }
}
