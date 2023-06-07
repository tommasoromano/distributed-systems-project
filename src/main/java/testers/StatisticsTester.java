package testers;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.sun.jersey.api.client.ClientResponse;

import adminserver.REST.RESTutils;
import utils.City;
import simulator.MeasurementRecord;

import java.util.ArrayList;

public class StatisticsTester {
  private static MqttClient mqttClient = null;

  public static void main(String[] args) {
    System.out.println("StatisticsTester");

    // startMQTT();
    String broker = "tcp://localhost:1883";
    String clientId = MqttClient.generateClientId();
    try {
        mqttClient = new MqttClient(broker, clientId);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true); // false = the broker stores all subscriptions for the client and all missed messages for the client that subscribed with a Qos level 1 or 2

        // Connect the client
        System.out.println("MQTT: " + clientId + " Connecting Broker " + broker);
        mqttClient.connect(connOpts);
        System.out.println("MQTT: " + clientId + " Connected Broker " + broker);

    } catch (MqttException me ) {
      me.printStackTrace();
    }

    // run multiple threads simultaneously
    for (int i = 0; i < 10; i++) {
      int id = i;
      Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            // random sleep
            int rand = ((int)Math.floor(Math.random() * (5 - 0 + 1) + 0));
            Thread.sleep(id * 1000);
            StatisticsTester.sendMQTTMessage(MeasurementRecord.toJson(new MeasurementRecord(
                    id,
                    System.currentTimeMillis(),
                    new ArrayList<Double>() {{
                      add(1.0*id);
                      add(1.0*id);
                      add(1.0*id);
                      add(1.0*id);
                      add(1.0*id);
                    }}
            )));
          } catch (Exception e) {
            // e.printStackTrace();
          }
        }
      });
      t.start();
    }

    // run multiple threads simultaneously for client rest
    for (int i = 0; i < 10; i++) {
      int id = i;
      Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            // random sleep
            int rand = ((int)Math.floor(Math.random() * (5 - 0 + 1) + 0));
            Thread.sleep((10+id) * 1000);
            ClientResponse response = RESTutils.RESTGetAvgLastNByRobotId(City.greenfieldCity.getId(), id, 5);
              if (response.getStatus() == 200) {
                System.out.println("Average of last 5 measurements of robot "+id+": "+response.getEntity(String.class));
                return;
              }
          } catch (Exception e) {
            // e.printStackTrace();
          }
        }
      });
      t.start();
    }
  }

  private static void sendMQTTMessage(String payload) {
    try {
      String topic = City.greenfieldCity.getName().toLowerCase()
        +"/pollution/district1";
      MqttMessage message = new MqttMessage(payload.getBytes());
      message.setQos(2);
      System.out.println("MQTT: Publishing message: "+message);
      mqttClient.publish(topic, message);
    } catch (MqttException e) {
      System.out.println("MQTT: Error: "+e.getMessage());
    }
  } 
}
