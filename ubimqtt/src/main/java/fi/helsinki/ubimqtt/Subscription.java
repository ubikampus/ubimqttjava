package fi.helsinki.ubimqtt;

import java.io.IOException;
import java.security.interfaces.ECPublicKey;

/**
 * Wrapper for the event when client subscribes to a topic
 * and starts to listen all the messages in that topic.
 */
public class Subscription {
    private String topic;
    private IUbiMessageListener listener;
    private ECPublicKey[] ecPublicKeys;
    private String[] decryptPrivateKey;

    public Subscription(String topic, IUbiMessageListener listener, String[] publicKeys, String[] decryptPrivateKey) throws IOException {
        this.topic = topic;
        this.listener = listener;
        this.decryptPrivateKey = decryptPrivateKey;

        if (publicKeys != null) {
            this.ecPublicKeys = new ECPublicKey[publicKeys.length];

            for (int i = 0; i < publicKeys.length; i++) {
                this.ecPublicKeys[i] = JwsHelper.createEcPublicKey(publicKeys[i]);
            }
        } else {
            this.ecPublicKeys = null;
        }
    }

    public Subscription(String topic, IUbiMessageListener listener, String[] publicKeys) throws IOException {
        this.topic = topic;
        this.listener = listener;
        this.decryptPrivateKey = null;

        if (publicKeys != null) {
            this.ecPublicKeys = new ECPublicKey[publicKeys.length];

            for (int i = 0; i < publicKeys.length; i++) {
                this.ecPublicKeys[i] = JwsHelper.createEcPublicKey(publicKeys[i]);
            }
        } else {
            this.ecPublicKeys = null;
        }
    }

    public IUbiMessageListener getListener() {
        return listener;
    }

    public ECPublicKey[] getEcPublicKeys() {
        return ecPublicKeys;
    }

    public void setEcPublicKeys(ECPublicKey[] ecPublicKeys) {
        this.ecPublicKeys = ecPublicKeys;
    }

    public String getTopic() {
        return topic;
    }

    public String[] getDecryptPrivateKey() {
        return decryptPrivateKey;
    }
}
