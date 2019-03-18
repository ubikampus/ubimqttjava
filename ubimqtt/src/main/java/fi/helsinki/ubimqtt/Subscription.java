package fi.helsinki.ubimqtt;

import java.io.IOException;
import java.security.interfaces.ECPublicKey;

public class Subscription {
    private String topic;
    private IUbiMessageListener listener;
    private ECPublicKey ecPublicKey;
    private long lastTimestamp;
    private String lastNonce;


    public Subscription(String topic, IUbiMessageListener listener, String publicKey) throws IOException {
        this.topic = topic;
        this.listener = listener;
        if (publicKey != null && publicKey != "") {
            this.ecPublicKey = JwsHelper.createEcPublicKey(publicKey);
            this.lastTimestamp = System.currentTimeMillis();
            this.lastNonce = "";
        } else {
            this.ecPublicKey = null;
        }
    }

    public IUbiMessageListener getListener() {
        return listener;
    }

    public ECPublicKey getEcPublicKey() {
        return ecPublicKey;
    }

    public void setEcPublicKey(ECPublicKey ecPublicKey) {
        this.ecPublicKey = ecPublicKey;
    }

    public String getTopic() {
        return topic;
    }

    public void setLastTimestamp(long lastTimestamp) {
        this.lastTimestamp = lastTimestamp;
    }

    public void setLastNonce(String lastNonce) {
        this.lastNonce = lastNonce;
    }

    public long getLastTimestamp() {
        return lastTimestamp;
    }

    public String getLastNonce() {
        return lastNonce;
    }
}
