package fi.helsinki.ubimqtt;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;

/**
 * Interface to indicate if operation succeeded or not and give the error if it didn't.
 *
 * @see org.eclipse.paho.client.mqttv3.IMqttActionListener
 */
public interface IUbiActionListener extends IMqttActionListener {
}
