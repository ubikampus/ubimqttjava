package fi.helsinki.ubimqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Listener for updating public keys during use.
 */
public class PublicKeyChangeListener implements IUbiMessageListener {
    private String mainListenerId;
    private IUbiActionListener originalCallback;
    private UbiMqtt ubiMqtt;
    private String mainTopic;
    private IUbiMessageListener mainListener;

    public PublicKeyChangeListener(UbiMqtt ubiMqtt, String mainTopic, IUbiMessageListener mainListener, IUbiActionListener originalCallback)  {
        this.ubiMqtt = ubiMqtt;
        this.mainTopic = mainTopic;
        this.mainListener = mainListener;
        this.originalCallback = originalCallback;

        this.mainListenerId = null;
    }

    public void messageArrived(String topic, MqttMessage message, String listenerId) throws Exception {
        if (mainListenerId != null) {
            System.out.println("PublicKeyChangeListener::onPublicKeyChanged() changing public key");
            ubiMqtt.updatePublicKey(mainTopic, mainListenerId, message.toString());
        } else {
            // This is the first time the public key arrives, subscribe to the main topic
            String[] publicKeys = {message.toString()};
            ubiMqtt.subscribeSigned(mainTopic, publicKeys, mainListener, originalCallback);
        }
    }
}
