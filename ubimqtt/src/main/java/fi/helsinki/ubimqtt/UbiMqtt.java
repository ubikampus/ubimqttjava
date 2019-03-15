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
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;


import java.security.Key;
import java.util.UUID;

public class UbiMqtt {

    private String clientId = null;
    private String serverAddress = null;

    private MqttAsyncClient client = null;


    public UbiMqtt(String serverAddress) {
        this.clientId = UUID.randomUUID().toString();

        if (serverAddress.startsWith("tcp://"))
            this.serverAddress = serverAddress;
        else
            this.serverAddress = "tcp://"+serverAddress;
    }

    public void connect(IMqttActionListener listener) {
        try {
            this.client = new MqttAsyncClient(serverAddress, clientId, new MemoryPersistence());

            MqttConnectOptions mqttClientOptions = new MqttConnectOptions();
            mqttClientOptions.setCleanSession(false);
            mqttClientOptions.setAutomaticReconnect(true);

            this.client.connect(mqttClientOptions, listener);
        }
        catch (MqttException e) {
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

    public void publish(IMqttActionListener actionListener, String topic, String message) {
        try {
            this.client.publish(topic, message.getBytes(), 1, false, null, actionListener);
        } catch (MqttException e) {
           actionListener.onFailure(null, e);
        }
    }

    public void publishSigned(IMqttActionListener actionListener, String topic, String message, String privateKey) {
        try {
            this.client.publish(topic, this.signMessage(message, privateKey).getBytes(), 1, false, null, actionListener);
        } catch (MqttException e) {
            actionListener.onFailure(null, e);
        }
    }

    public void subscribe(IMqttActionListener actionListener, String topic, IMqttMessageListener listener) {
        try {
            this.client.subscribe(topic, 1, null, actionListener, listener);
        } catch (MqttException e) {
            actionListener.onFailure(null, e);
        }
    }

    public void subscribeSigned(IMqttActionListener actionListener, String topic, String publicKey, IMqttMessageListener listener) {
      //ToDo: write a IMqttMessageListener that checks the signatures
    }

    public void subscribeFromPublisher(String topic, String publisherName, IMqttMessageListener listener) {
       //ToDo: subscribe to the key changes of the publisher
    }

    private String signMessage(String message, String pemEncodedRSAPrivateKey) {

       return message;
    }

}
