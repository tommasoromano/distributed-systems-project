package adminserver.statistics;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import adminserver.AdministratorServer;
import utils.City;
import simulator.MeasurementRecord;
import utils.Config;

public class StatisticSubscriber implements Runnable {

  private Map<Integer,Integer> sensorCounter = new HashMap<Integer,Integer>();

  private City city;
  public StatisticSubscriber(City city) {
    this.city = city;
  }
  public StatisticSubscriber() {
    this.city = AdministratorServer.getInstance().getCity();
  }

  @Override
  public void run() {
    MqttClient client;
    String broker = "tcp://localhost:1883";
    String clientId = MqttClient.generateClientId();
    String[] subTopicArray = new String[this.city.getDistricts().size()];
    int[] subQosArray = new int[this.city.getDistricts().size()];
    for (int i = 0; i < this.city.getDistricts().size(); i++) {
      subTopicArray[i] = cityDistrictToTopic(city, i);
      subQosArray[i] = 2;
    }

    try {
        client = new MqttClient(broker, clientId);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true); // false = the broker stores all subscriptions for the client and all missed messages for the client that subscribed with a Qos level 1 or 2

        // Connect the client
        System.out.println("MQTT Subscriber: " + clientId + " Connecting Broker " + broker);
        client.connect(connOpts);
        System.out.println("MQTT Subscriber: " + clientId + " Connected " + Thread.currentThread().getId());

        // Callback
        client.setCallback(new MqttCallback() {

            public void messageArrived(String topic, MqttMessage message) {
                // Called when a message arrives from the server that matches any subscription made by the client
                String time = new Timestamp(System.currentTimeMillis()).toString();
                String receivedMessage = new String(message.getPayload());
                try {
                  MeasurementRecord measurementRecord = MeasurementRecord.fromJson(receivedMessage);
                  AdministratorServer.getInstance().getStatistics()
                    .addMeasurement(measurementRecord);

                  // print
                  int id = measurementRecord.getRobotId();
                  if (sensorCounter.get(id) == null || sensorCounter.get(id) % Config.PRINT_SENSOR_EVERY == 0) {
                    sensorCounter.put(id, 0);
                    System.out.println("MQTT Subscriber: " + clientId +" Received a Message." +
                            "\n\tTime: " + time +
                            "\tTopic: " + topic +
                            "\tMessage: " + receivedMessage +
                            "\tQoS:     " + message.getQos());
                  }
                  sensorCounter.put(id, sensorCounter.get(id) + 1);

                } catch (Exception e) {
                  System.out.println("MQTT Subscriber: Error parsing JSON: " + e.getMessage());
                }
            }

            public void connectionLost(Throwable cause) {
                System.out.println("MQTT Subscriber: " + clientId + " Connectionlost! cause:" + cause.getMessage()+ "-  Thread PID: " + Thread.currentThread().getId());
            }

            public void deliveryComplete(IMqttDeliveryToken token) {
                if (token.isComplete()) {
                    System.out.println("MQTT Subscriber: " + clientId + " Message delivered - Thread PID: " + Thread.currentThread().getId());
                }
            }

        });

        System.out.println("MQTT Subscriber: " + clientId + " Subscribing ... - Thread PID: " + Thread.currentThread().getId());
        client.subscribe(subTopicArray,subQosArray);
        System.out.println("MQTT Subscriber: " + clientId + " Subscribed to topics : " + Arrays.toString(subTopicArray));

    } catch (MqttException me ) {
        System.out.println("reason " + me.getReasonCode());
        System.out.println("msg " + me.getMessage());
        System.out.println("loc " + me.getLocalizedMessage());
        System.out.println("cause " + me.getCause());
        System.out.println("excep " + me);
        me.printStackTrace();
    }
  }

  public static String cityDistrictToTopic(City city, int districtId) {
    return city.getName().toLowerCase()+"/pollution/district" + districtId;
  }

}
