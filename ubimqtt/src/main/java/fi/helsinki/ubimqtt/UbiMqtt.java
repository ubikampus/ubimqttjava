package fi.helsinki.ubimqtt;

import com.nimbusds.jose.JOSEException;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

/**
 * Main class to implement actual Mqtt client and all it's commonly needed operations.
 * Handling some important configurations so that they are out of the box for the user.
 */
public class UbiMqtt {
    public static final int DEFAULT_BUFFER_WINDOW_IN_SECONDS = 60;
    public static final String PUBLISHERS_PREFIX = "publishers/";

    private String clientId;
    private String serverAddress;

    private MqttAsyncClient client = null;
    private MessageValidator messageValidator;

    private int listenerCounter = 0;

    private Map<String, Map<String, Subscription>> subscriptions;
    private Vector<PublicKeyChangeListener> publicKeyChangeListeners;

    private ArrayList<Map.Entry<String, Subscription>> getSubscriptionsForTopic(String topic) {
        ArrayList<Map.Entry<String, Subscription>> ret = new ArrayList<>();

        for (Map.Entry<String, Map<String, Subscription>> entry : subscriptions.entrySet()) {
            if (MqttTopic.isMatched(entry.getKey(), topic)) {
                ret.addAll(entry.getValue().entrySet());
            }
        }

        return ret;
    }

    private IMqttMessageListener messageListener = new IMqttMessageListener() {
        @Override
        public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
            ArrayList<Map.Entry<String, Subscription>> subscriptionsForTopic = getSubscriptionsForTopic(topic);

            for (Map.Entry<String, Subscription> next : subscriptionsForTopic) {
                if (next.getValue().getEcPublicKeys() != null) {
                    // This is a topic where signed messages are expected, try if the signature matches some of the public keys
                    ECPublicKey[] tempKeys = next.getValue().getEcPublicKeys();

                    for (ECPublicKey tempKey : tempKeys) {
                        if (messageValidator.validateMessage(mqttMessage.toString(), tempKey)) {
                            next.getValue().getListener().messageArrived(topic, mqttMessage, next.getKey());
                            break;
                        }
                    }
                } else {
                    if (next.getValue().getDecryptPrivateKey() != null) {
                        for (String privateKey : next.getValue().getDecryptPrivateKey()) {
                            try {
                                String decryptMessage = JwsHelper.decryptMessage(mqttMessage.toString(), privateKey);
                                mqttMessage.setPayload(decryptMessage.getBytes());
                                next.getValue().getListener().messageArrived(topic, mqttMessage, next.getKey());

                                break;
                            } catch (RuntimeException ex) {
                                ex.printStackTrace();
                            }
                        }
                    } else {
                        next.getValue().getListener().messageArrived(topic, mqttMessage, next.getKey());
                    }
                }
            }
        }
    };

    void updatePublicKey(String topic, String listenerId, String publicKey) throws IOException {
        if (subscriptions.containsKey(topic) && subscriptions.get(topic).containsKey(listenerId)) {
            ECPublicKey[] tempKeys = new ECPublicKey[1];
            tempKeys[0] = JwsHelper.createEcPublicKey(publicKey);
            subscriptions.get(topic).get(listenerId).setEcPublicKeys(tempKeys);
        }
    }

    private void addSubscription(IUbiActionListener actionListener, String topic, String[] publicKeys, IUbiMessageListener listener) {
        try {
            if (!subscriptions.containsKey(topic)) {
                subscriptions.put(topic, Collections.synchronizedMap(new HashMap<>()));
            }

            String listenerId = Integer.toString(listenerCounter);
            listenerCounter++;

            subscriptions.get(topic).put(listenerId, new Subscription(topic, listener, publicKeys));

            this.client.subscribe(topic, 1, null, actionListener, messageListener);
        } catch (Exception e) {
            e.printStackTrace();
            actionListener.onFailure(null, e);
        }
    }

    private void addSubscriptionEncrypted(IUbiActionListener actionListener, String topic, String[] publicKeys,
                                          String[] decryptPrivateKey, IUbiMessageListener listener) {

        try {
            if (!subscriptions.containsKey(topic)) {
                subscriptions.put(topic, Collections.synchronizedMap(new HashMap<>()));
            }

            String listenerId = Integer.toString(listenerCounter);
            listenerCounter++;

            subscriptions.get(topic).put(listenerId, new Subscription(topic, listener, publicKeys, decryptPrivateKey));

            this.client.subscribe(topic, 1, null, actionListener, messageListener);
        } catch (Exception e) {
            e.printStackTrace();
            actionListener.onFailure(null, e);
        }
    }

    private String signMessage(String message, String privateKey) throws IOException, JOSEException, ParseException {
        return JwsHelper.signMessage(message, privateKey);
    }

    /**
     * Constructs a Ubimqtt instance with default bufferWindowInSeconds but does not connect to a server.
     *
     * @param serverAddress the Mqtt server to use
     */
    public UbiMqtt(String serverAddress) {
        this(serverAddress, DEFAULT_BUFFER_WINDOW_IN_SECONDS);
    }

    /**
     * Constructs a Ubimqtt instance but does not connect to a server.
     *
     * @param serverAddress the Mqtt server to use
     * @param bufferWindowInSeconds the maximum acceptable age for signed messages, older signed messages will be discarded
     */
    public UbiMqtt(String serverAddress, int bufferWindowInSeconds) {
        this.clientId = UUID.randomUUID().toString();
        this.messageValidator = new MessageValidator(bufferWindowInSeconds);

        this.subscriptions = Collections.synchronizedMap(new HashMap<>());
        this.publicKeyChangeListeners = new Vector<>();

        if (serverAddress.startsWith("tcp://")) {
            this.serverAddress = serverAddress;
        } else {
            this.serverAddress = "tcp://" + serverAddress;
        }
    }

    /**
     * Connecs to the Mqtt server the address of which was given as a constructor parameter.
     *
     * @param actionListener the listener to call upon connection or error
     */
    public void connect(IUbiActionListener actionListener) {
        try {
            this.client = new MqttAsyncClient(serverAddress, clientId, new MemoryPersistence());

            MqttConnectOptions mqttClientOptions = new MqttConnectOptions();
            mqttClientOptions.setCleanSession(false);
            mqttClientOptions.setAutomaticReconnect(true);

            this.client.connect(mqttClientOptions, actionListener);
        } catch (MqttException e) {
            actionListener.onFailure(null, e);
        }
    }

    /**
     * Disconnects from the Mqtt server.
     *
     * @param actionListener the callback to call upon successful disconnection or error
     */
    public void disconnect(IUbiActionListener actionListener) {
        try {
            this.client.disconnect();
            actionListener.onSuccess(null);
        } catch (MqttException e) {
            actionListener.onFailure(null, e);
        }

    }

    /**
     * Publishes a message on the connected Mqtt server.
     *
     * @param topic the Mqtt topic to publish to
     * @param message the message to publish
     * @param qos the Mqtt qos to use
     * @param retained publish the message as a retained Mqtt message if true
     * @param actionListener the callback to call upon success or error
     */
    public void publish(String topic, String message, int qos, boolean retained, IUbiActionListener actionListener) {
        try {
            this.client.publish(topic, message.getBytes(), qos, retained, null, actionListener);
        } catch (MqttException e) {
            actionListener.onFailure(null, e);
        }
    }

    /**
     * Publishes a message on the connected Mqtt server with default qos = 1 and retained = false.
     *
     * @param topic the Mqtt topic to publish to
     * @param message the message to publish
     * @param actionListener the callback to call upon success or error
     */
    public void publish(String topic, String message, IUbiActionListener actionListener) {
        publish(topic, message, 1, false, actionListener);
    }


    /**
     * Publishes a signed message on the connected Mqtt server.
     *
     * @param topic the Mqtt topic to publish to
     * @param message the message to publish
     * @param qos the Mqtt qos to use
     * @param retained publish the message as a retained Mqtt message if true
     * @param privateKey the private key in .pem format to sign the message with
     * @param actionListener the callback to call upon success or error
     */
    public void publishSigned(String topic, String message, int qos, boolean retained, String privateKey, IUbiActionListener actionListener) {
        try {
            this.client.publish(topic, this.signMessage(message, privateKey).getBytes(), qos, retained, null, actionListener);
        } catch (Exception e) {
            actionListener.onFailure(null, e);
        }
    }

    /**
     * Publishes a signed message on the connected Mqtt server with default qos = 1 and retained = false.
     *
     * @param topic the Mqtt topic to publish to
     * @param message the message to publish
     * @param privateKey the private key in .pem format to sign the message with
     * @param actionListener the callback to call upon success or error
     */
    public void publishSigned(String topic, String message, String privateKey, IUbiActionListener actionListener) {
        publishSigned(topic, message, 1, false, privateKey, actionListener);
    }

    /**
     * Publishes a message on the connected Mqtt server.
     * Encrypting all the messages which are going to be published.
     *
     * @param topic the Mqtt topic to publish to
     * @param message the message to publish
     * @param qos the Mqtt qos to use
     * @param retained publish the message as a retained Mqtt message if true
     * @param encryptPublicKey public key for the encryption
     * @param actionListener the callback to call upon success or error
     */
    public void publishEncrypted(String topic, String message, int qos, boolean retained,
                                 String encryptPublicKey, IUbiActionListener actionListener) {

        try {
            this.client.publish(topic, this.encryptMessage(message, encryptPublicKey).getBytes(), qos, retained, null, actionListener);
        } catch (Exception e) {
            actionListener.onFailure(null, e);
        }
    }

    /**
     * Publishes a message on the connected Mqtt server with default qos = 1 and retained = false.
     * Encrypting all the messages which are going to be published.
     *
     * @param topic the Mqtt topic to publish to
     * @param message the message to publish
     * @param encryptPublicKey public key for the encryption
     * @param actionListener the callback to call upon success or error
     */
    public void publishEncrypted(String topic, String message, String encryptPublicKey, IUbiActionListener actionListener) {
        publishEncrypted(topic, message, 1, false, encryptPublicKey, actionListener);
    }

    private String encryptMessage(String message, String publicKey) throws IOException, JOSEException {
        return JwsHelper.encryptMessage(message, publicKey);
    }

    /**
     * Subscribes to a Mqtt topic on the connected Mqtt server.
     *
     * @param topic the Mqtt topic to subscribe to
     * @param listener the listener function to call whenever a message matching the topic arrives
     * @param actionListener the listener to be called upon successful subscription or error
     */
    public void subscribe(String topic, IUbiMessageListener listener, IUbiActionListener actionListener) {
        addSubscription(actionListener, topic, null, listener);
    }

    /**
     * Subscribes to a Mqtt topic on the connected Mqtt server decrypting all the messages that arrive.
     *
     * @param topic the Mqtt topic to subscribe to
     * @param decryptPrivateKey the private keys the messages is decrypted against
     * @param listener the listener function to call whenever a message matching the topic arrives
     * @param actionListener the listener to be called upon successful subscription or error
     */
    public void subscribeEncrypted(String topic, String[] decryptPrivateKey, IUbiMessageListener listener, IUbiActionListener actionListener) {
        addSubscriptionEncrypted(actionListener, topic, null, decryptPrivateKey, listener);
    }

    /**
     * Subscribes to messages signed by particular keypairs on a Mqtt topic on the connected Mqtt server.
     *
     * @param topic the Mqtt topic to subscribe to
     * @param publicKeys the public keys the messages are checked against
     * @param listener the listener function to call whenever a message matching the topic and signed with one of the publicKeys arrives
     * @param actionListener the callback to be called upon successful subscription or error
     */
    public void subscribeSigned(String topic, String[] publicKeys, IUbiMessageListener listener, IUbiActionListener actionListener) {
        addSubscription(actionListener, topic, publicKeys, listener);
    }

    /**
     * Subscribes to messages on a Mqtt topic on the connected Mqtt server signed by a known publiser The public key of the publisher
     * is used for recognizing the messages originating from the publisher. The public key of the publisher is fetched from the Mqtt
     * topic publishers/publishername/publicKey and kept up-to-date with the help of a regular Mqtt subscription.
     *
     * @param topic the Mqtt topic to subscribe to
     * @param publisherName the name of the known publisher
     * @param listener the listener to call whenever a message matching the topic and signed with the publicKey arrives
     * @param actionListener the callback to be called upon successful subscription or error
     */
    public void subscribeFromPublisher(String topic, String publisherName, IUbiMessageListener listener, IUbiActionListener actionListener) {
        PublicKeyChangeListener publicKeyChangeListener = new PublicKeyChangeListener(this, topic, listener, actionListener);
        publicKeyChangeListeners.add(publicKeyChangeListener);

        // Subscribe to the public key of the publisher
        String publicKeyTopic = PUBLISHERS_PREFIX + publisherName + "/publicKey";

        this.subscribe(publicKeyTopic, publicKeyChangeListener, new IUbiActionListener() {
            @Override
            public void onSuccess(IMqttToken iMqttToken) {
            }

            @Override
            public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                actionListener.onFailure(iMqttToken, throwable);
            }
        });
    }
}
