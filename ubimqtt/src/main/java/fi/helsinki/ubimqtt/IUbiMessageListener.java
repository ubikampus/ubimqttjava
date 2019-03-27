package fi.helsinki.ubimqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

public interface IUbiMessageListener {
    public void messageArrived(String topic, MqttMessage mqttMessage, String listenerId) throws Exception;
}
