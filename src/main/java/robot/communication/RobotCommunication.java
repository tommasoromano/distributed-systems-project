package robot.communication;

import javax.ws.rs.core.Response;

import adminserver.REST.RESTutils;
import adminserver.REST.beans.InsertRobotBean;
import utils.MeasurementRecord;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.sun.jersey.api.client.ClientResponse;

import robot.Robot;
import utils.City;

public class RobotCommunication {

  private MqttClient client;

  private Thread grpcThread;

  public RobotCommunication() {
  }

  /**
   * requires the robot to be initialized
   * @return
   */
  public InsertRobotBean joinNetwork() {

    Robot robot = Robot.getInstance();

    System.out.println("Communication: Robot "+robot.getId()+" is trying to join the network.");
    InsertRobotBean insertRobotBean = null;

    ClientResponse response = RESTutils.RESTPostRobot(
          robot.getCityId(),
          robot.getId(), 
          robot.getIpAddress(), 
          robot.getPortNumber()
        );

    if (response.getStatus() != Response.Status.OK.getStatusCode()) {
      System.out.println("Communication: Robot "+robot.getId()+" failed to join the network."
          + "\n\tError code: " + response.getStatus());
      throw new RuntimeException("Robot "+robot.getId()+" failed to join the network." 
          + "\n\tError code: " + response.getStatus());
    }

    System.out.println("Communication: This Robot "+robot.getId()+" joined the network.");

    try {
      insertRobotBean = response.getEntity(InsertRobotBean.class);
    } catch (Exception e) {
      System.out.println("Communication: Error: "+e.getMessage());
      robot.disconnect();
      return null;
    }

    // create mqtt
    startMQTT();

    // send message to all robots
    this.grpcThread = new Thread(new GRPCServer());
    this.grpcThread.start();

    return insertRobotBean;

  }

  ////////////////////////////////////////////////////////////
  // MQTT
  ////////////////////////////////////////////////////////////

  private void startMQTT() {
    String broker = "tcp://localhost:1883";
    String clientId = MqttClient.generateClientId();
    try {
        this.client = new MqttClient(broker, clientId);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true); // false = the broker stores all subscriptions for the client and all missed messages for the client that subscribed with a Qos level 1 or 2

        // Connect the client
        System.out.println("MQTT: " + clientId + " Connecting Broker " + broker);
        client.connect(connOpts);
        System.out.println("MQTT: " + clientId + " Connected Broker " + broker);

    } catch (MqttException me ) {
      System.out.println("MQTT: Error:");
        System.out.println("\treason " + me.getReasonCode());
        System.out.println("\tmsg " + me.getMessage());
        System.out.println("\tloc " + me.getLocalizedMessage());
        System.out.println("\tcause " + me.getCause());
        System.out.println("\texcep " + me);
        
        Robot.getInstance().disconnect();
    }
  }

  public void sendAvgPollutionLevel(
    long timestamp,
    String sensorId,
    double value
    ) {
      MeasurementRecord measurementRecord = new MeasurementRecord(
        Robot.getInstance().getId(), 
        timestamp,
        sensorId,
        value
      );

      String payload = measurementRecord.toJson();
      sendMQTTMessage(payload);
  }

  private void sendMQTTMessage(String payload) {
    Robot robot = Robot.getInstance();
    try {
      String topic = City.getCityById(robot.getCityId()).getName().toLowerCase()
        +"/pollution/district"+robot.getDistrictId();
      MqttMessage message = new MqttMessage(payload.getBytes());
      message.setQos(2);
      System.out.println("MQTT: Publishing message: "+message);
      client.publish(topic, message);
    } catch (MqttException e) {
      System.out.println("MQTT: Error: "+e.getMessage());
    }
  }


  ////////////////////////////////////////////////////////////
  // gRPC
  ////////////////////////////////////////////////////////////



  ////////////////////////////////////////////////////////////
  // DISCONNECT
  ////////////////////////////////////////////////////////////

  public void disconnect() {

    Robot robot = Robot.getInstance();
    System.out.println("Communication: Robot "+robot.getId()+" is trying to leave the network.");
    
    // send message to the admin
    ClientResponse response = RESTutils.RESTDeleteRobot(robot.getCityId(), robot.getId());
    if (response.getStatus() != Response.Status.OK.getStatusCode()) {
      System.out.println("Communication: Robot "+robot.getId()+" failed to leave the network."
          + "\n\tError code: " + response.getStatus());
      throw new RuntimeException("Robot "+robot.getId()+" failed to leave the network." 
          + "\n\tError code: " + response.getStatus());
    }

    // send messages to the other robots

    System.out.println("Communication: Robot "+robot.getId()+" left the network.");

  }

}
