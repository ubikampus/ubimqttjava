package fi.helsinki.ubimqtt;

import java.io.IOException;
import java.security.interfaces.ECPublicKey;

public class Subscription {
    private String topic;
    private IUbiMessageListener listener;
    private ECPublicKey[] ecPublicKeys;



    public Subscription(String topic, IUbiMessageListener listener, String[] publicKeys) throws IOException {
        this.topic = topic;
        this.listener = listener;
        if (publicKeys != null) {
            this.ecPublicKeys = new ECPublicKey[publicKeys.length];

            for (int i=0; i<publicKeys.length; i++) {
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

}
