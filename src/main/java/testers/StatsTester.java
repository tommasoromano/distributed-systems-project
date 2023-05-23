package testers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Test;

import utils.MeasurementRecord;
import utils.City;

public class StatsTester {
  public static void main(String[] args) {
    new StatsTester().statsTets();
  }
  @Test
  public void statsTets() {
    City city = City.greenfieldCity;
    MqttClient client;
    String broker = "tcp://localhost:1883";
    String clientId = MqttClient.generateClientId();
    String pubTopic = "greenfield/pollution/district1";
    int pubQos = 2;

    try {
        client = new MqttClient(broker, clientId);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true); // false = the broker stores all subscriptions for the client and all missed messages for the client that subscribed with a Qos level 1 or 2

        // Connect the client
        System.out.println(clientId + " Connecting Broker " + broker);
        client.connect(connOpts);
        System.out.println(clientId + " Connected " + Thread.currentThread().getId());

        for (int i = 0; i < 100; i++) {
          int district = ((int)Math.floor(Math.random() * (3 - 1 + 1) + 1));
          int id = ((int)Math.floor(Math.random() * (10 - 1 + 1) + 1));
          List<Double> avgs = new ArrayList<Double>();
          avgs.add((double)Math.random()*100000);
          avgs.add((double)Math.random()*100000);
          avgs.add((double)Math.random()*100000);
          byte[] payload = MeasurementRecord.toJson(new MeasurementRecord(
            id, 
            System.currentTimeMillis(),
            avgs
          )).getBytes();
          MqttMessage message = new MqttMessage(payload);
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
