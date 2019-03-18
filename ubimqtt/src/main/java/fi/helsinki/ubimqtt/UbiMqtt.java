package fi.helsinki.ubimqtt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.simple.parser.ParseException;


import java.io.IOException;
import java.security.Key;
import java.security.interfaces.ECPublicKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

public class UbiMqtt {
    public static final int DEFAULT_MAXIMUM_REPLAY_BUFFER_SIZE = 1000;

    public static final String PUBLISHERS_PREFIX = "publishers/";

    private String clientId = null;
    private String serverAddress = null;

    private MqttAsyncClient client = null;
    private MessageValidator messageValidator;

    int listenerCounter = 0;

    private Map<String, Map<String, Subscription>> subscriptions;
    private Vector<PublicKeyChangeListener> publicKeyChangeListeners;

    private IMqttMessageListener messageListener = new IMqttMessageListener() {
        @Override
        public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
            if (subscriptions.containsKey(topic)) {
                Iterator<Map.Entry<String, Subscription>> iter = subscriptions.get(topic).entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String, Subscription> entry = iter.next();
                    if (entry.getValue().getEcPublicKeys() != null) {
                        // This is a topic where signed messages are expected, try if the signature matches some of the public keys
                        ECPublicKey[] tempKeys = entry.getValue().getEcPublicKeys();
                        for (int i=0; i< tempKeys.length; i++) {
                            if (messageValidator.validateMessage(mqttMessage.toString(), tempKeys[i])) {
                                entry.getValue().getListener().messageArrived(topic, mqttMessage, entry.getKey());
                                break;
                            }
                        }
                    } else {
                        entry.getValue().getListener().messageArrived(topic, mqttMessage, entry.getKey());
                    }
                }
            }
        }
    };

    public UbiMqtt(String serverAddress) {
        this.clientId = UUID.randomUUID().toString();
        this.messageValidator = new MessageValidator(DEFAULT_MAXIMUM_REPLAY_BUFFER_SIZE);

        this.subscriptions = Collections.synchronizedMap(new HashMap<String, Map<String, Subscription>>());
        this.publicKeyChangeListeners = new Vector<PublicKeyChangeListener>();

        if (serverAddress.startsWith("tcp://"))
            this.serverAddress = serverAddress;
        else
            this.serverAddress = "tcp://" + serverAddress;

    }

    public UbiMqtt(String serverAddress, int maximumReplayBufferSize) {
        this.clientId = UUID.randomUUID().toString();
        this.messageValidator = new MessageValidator(maximumReplayBufferSize);

        this.subscriptions = Collections.synchronizedMap(new HashMap<String, Map<String, Subscription>>());
        this.publicKeyChangeListeners = new Vector<PublicKeyChangeListener>();

        if (serverAddress.startsWith("tcp://"))
            this.serverAddress = serverAddress;
        else
            this.serverAddress = "tcp://" + serverAddress;

    }

    public void connect(IMqttActionListener listener) {
        try {
            this.client = new MqttAsyncClient(serverAddress, clientId, new MemoryPersistence());

            MqttConnectOptions mqttClientOptions = new MqttConnectOptions();
            mqttClientOptions.setCleanSession(false);
            mqttClientOptions.setAutomaticReconnect(true);

            this.client.connect(mqttClientOptions, listener);
        } catch (MqttException e) {
            listener.onFailure(null, e);
        }
    }

    public void disconnect(IMqttActionListener actionListener) {
        try {
            this.client.disconnect();
            actionListener.onSuccess(null);
        } catch (MqttException e) {
            actionListener.onFailure(null, e);
        }

    }

    public void publish(IMqttActionListener actionListener, String topic, String message, int qos, boolean retained) {
        try {
            this.client.publish(topic, message.getBytes(), qos, retained, null, actionListener);
        } catch (MqttException e) {
            actionListener.onFailure(null, e);
        }
    }

    public void publish(IMqttActionListener actionListener, String topic, String message) {
        publish(actionListener, topic, message, 1,false);
    }


    public void publishSigned(IMqttActionListener actionListener, String topic, String message, String privateKey, int qos, boolean retained) {
        try {
            this.client.publish(topic, this.signMessage(message, privateKey).getBytes(), qos, retained, null, actionListener);
        } catch (Exception e) {
            actionListener.onFailure(null, e);
        }
    }

    public void publishSigned(IMqttActionListener actionListener, String topic, String message, String privateKey) {
        publishSigned(actionListener, topic, message, privateKey, 1, false);
    }

    protected void updatePublicKey(String topic, String listenerId, String publicKey) throws IOException {
        if (subscriptions.containsKey(topic) && subscriptions.get(topic).containsKey(listenerId)) {
            ECPublicKey[] tempKeys = new ECPublicKey[1];
            tempKeys[0] = JwsHelper.createEcPublicKey(publicKey);
            subscriptions.get(topic).get(listenerId).setEcPublicKeys(tempKeys);
        }
    }

    private void addSubscription(IMqttActionListener actionListener, String topic, String[] publicKeys, IUbiMessageListener listener) {
        try {
            if (!subscriptions.containsKey(topic))
                subscriptions.put(topic, Collections.synchronizedMap(new HashMap<String, Subscription>()));

            String listenerId = listenerCounter + "";
            listenerCounter++;

            subscriptions.get(topic).put(listenerId, new Subscription(topic, listener, publicKeys));

            this.client.subscribe(topic, 1, null, actionListener, messageListener);
        } catch (Exception e) {
            e.printStackTrace();
            actionListener.onFailure(null, e);
        }
    }

    public void subscribe(IMqttActionListener actionListener, String topic, IUbiMessageListener listener) {
        addSubscription(actionListener, topic, null, listener);
    }

    public void subscribeSigned(IMqttActionListener actionListener, String topic, String[] publicKeys, IUbiMessageListener listener) {
        addSubscription(actionListener, topic, publicKeys, listener);
    }

    public void subscribeFromPublisher(IMqttActionListener actionListener, String topic, String publisherName, IUbiMessageListener listener) {
        PublicKeyChangeListener publicKeyChangeListener = new PublicKeyChangeListener(this, topic, listener, actionListener);
        publicKeyChangeListeners.add(publicKeyChangeListener);

        //subscribe to the public key of the publisher
        String publicKeyTopic = PUBLISHERS_PREFIX + publisherName + "/publicKey";

        this.subscribe(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken iMqttToken) {
            }

            @Override
            public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                actionListener.onFailure(iMqttToken, throwable);
            }
        }, publicKeyTopic, publicKeyChangeListener);

    }

    private String signMessage(String message, String privateKey) throws IOException, JOSEException {
        return JwsHelper.signMessage(message, privateKey);
    }

    private boolean verifyMessage(String message, String publicKey) throws ParseException, JOSEException, java.text.ParseException, IOException {
        return JwsHelper.verifySignature(message, publicKey);
    }
}
