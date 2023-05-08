package testers;

import java.sql.Timestamp;
import java.util.Arrays;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Test;

import adminserver.City;
import simulator.Measurement;

public class StatsTester {
  @Test
  public void statsTets() {
    City city = City.greenfieldCity;
    MqttClient client;
    String broker = "tcp://localhost:1883";
    String clientId = MqttClient.generateClientId();
    String pubTopic = "greenfield/pollution/district1";
    String[] subTopicArray = new String[city.getDistricts().size()];
    int[] subQosArray = new int[city.getDistricts().size()];
    for (int i = 0; i < city.getDistricts().size(); i++) {
      subTopicArray[i] = city.getName().toLowerCase()+"/pollution/district" + city.getDistricts().get(i).getId();
      subQosArray[i] = 2;
    }
    int pubQos = 2;

    try {
        client = new MqttClient(broker, clientId);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true); // false = the broker stores all subscriptions for the client and all missed messages for the client that subscribed with a Qos level 1 or 2

        // Connect the client
        System.out.println(clientId + " Connecting Broker " + broker);
        client.connect(connOpts);
        System.out.println(clientId + " Connected " + Thread.currentThread().getId());

        for (int i = 0; i < 10; i++) {
          String payload = new Measurement("id", "type", Math.random()*100000, System.currentTimeMillis()).toString();
          MqttMessage message = new MqttMessage(payload.getBytes());
          // Set the QoS on the Message
          message.setQos(pubQos);
          System.out.println(clientId + " Publishing message: " + payload + " ...");
          client.publish(pubTopic, message);
          System.out.println(clientId + " Message published - Thread PID: " + Thread.currentThread().getId());
        }

        // client.disconnect();

    } catch (MqttException me ) {
        System.out.println("reason " + me.getReasonCode());
        System.out.println("msg " + me.getMessage());
        System.out.println("loc " + me.getLocalizedMessage());
        System.out.println("cause " + me.getCause());
        System.out.println("excep " + me);
        me.printStackTrace();
    }
  }
}
