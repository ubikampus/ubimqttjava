package fi.helsinki.ubimqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Interface for implementing the actions that are wanted to be taken
 * after valid messages arrives to the topic that subscription listens with this listener.
 */
public interface IUbiMessageListener {
    void messageArrived(String topic, MqttMessage mqttMessage, String listenerId) throws Exception;
}
