package fi.helsinki.ubimqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public interface IUbiMessageListener {
    void messageArrived(String topic, MqttMessage mqttMessage, String listenerId) throws Exception;
}
