package robot.communication;

import javax.ws.rs.core.Response;

import adminserver.REST.RESTutils;
import adminserver.REST.beans.InsertRobotBean;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import protos.network.NetworkMessage;
import protos.network.NetworkResponse;
import protos.network.NetworkResult;
import protos.network.NetworkServiceGrpc;
import protos.network.NetworkServiceGrpc.NetworkServiceBlockingStub;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.sun.jersey.api.client.ClientResponse;

import robot.Robot;
import utils.City;
import utils.Config;

public class RobotCommunication {

  private MqttClient client;

  private Thread grpcThread;

  private boolean connectedToAdmin = false;
  private boolean connectedToMQTT = false;
  private boolean connectedToGRPC = false;

  private int sensorMsgCounter = 0;

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
      System.out.println("Communication: Robot "+robot.getId()+" failed to register to admin."
          + "\n\tError code: " + response.getStatus());
      throw new RuntimeException("Robot "+robot.getId()+" failed to register to admin." 
          + "\n\tError code: " + response.getStatus());
    }
    System.out.println("Communication: This Robot "+robot.getId()+" registered to admin.");
    this.connectedToAdmin = true;

    try {
      insertRobotBean = response.getEntity(InsertRobotBean.class);
    } catch (Exception e) {
      System.out.println("Communication: Error: "+e.getMessage());
      robot.disconnect();
      return null;
    }
    System.out.println("Communication: Robot "+robot.getId()+" received response from admin, robots: "+insertRobotBean.getRobots().size());


    // create mqtt
    startMQTT();
    this.connectedToMQTT = true;

    // start grpc
    this.grpcThread = new Thread(new GRPCServer());
    this.grpcThread.start();
    while(!this.connectedToGRPC) {
      try {
        Thread.sleep(1*1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

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

  public void sendPollutionLevel(
    String payload
    ) {
      sendMQTTMessage(payload);
  }

  private void sendMQTTMessage(String payload) {
    Robot robot = Robot.getInstance();
    try {
      String topic = City.getCityById(robot.getCityId()).getName().toLowerCase()
        +"/pollution/district"+robot.getDistrictId();
      MqttMessage message = new MqttMessage(payload.getBytes());
      message.setQos(2);
      if (this.sensorMsgCounter % Config.PRINT_SENSOR_EVERY == 0) {
        System.out.println("MQTT: Publishing message: "+message);
      }
      client.publish(topic, message);
      this.sensorMsgCounter++;
    } catch (MqttException e) {
      System.out.println("MQTT: Error: "+e.getMessage());
    }
  }


  ////////////////////////////////////////////////////////////
  // gRPC
  ////////////////////////////////////////////////////////////

  public void setConnectedToGRPC(boolean connectedToGRPC) {
    this.connectedToGRPC = connectedToGRPC;
  }

  public NetworkResponse sendMessageToRobot(NetworkMessage msg, int recipientPort) {
    return this.sendGRPCMessage(msg, recipientPort);
  }

  private NetworkResponse sendGRPCMessage(NetworkMessage request, int recipientPort) {
    
    try {

      final ManagedChannel channel =
          ManagedChannelBuilder.forTarget("localhost:"+recipientPort).usePlaintext().build();

      NetworkServiceBlockingStub stub = NetworkServiceGrpc.newBlockingStub(channel);

      System.out.println("gRPC: Sending message: "+request+", to port:"+recipientPort);

      NetworkResponse response = stub.sendNetworkMessage(request);

      System.out.println("gRPC: Response: "+response);

      channel.shutdown();

      return response;

    } catch (Exception e) {
      System.out.println("gRPC: Send message Error: "+e.getMessage());
      // e.printStackTrace();
      return null;
    }
  }

  public NetworkResponse createResponseForRobotMessage(NetworkMessage message) {
    return Robot.getInstance().getNetwork().createResponseForRobotMessage(message);
  }

  ////////////////////////////////////////////////////////////
  // MAINTENENCE
  ////////////////////////////////////////////////////////////

  public void startMaintenance() {
    // TODO
  }

  public void endMaintenance() {
    // TODO
  }

  ////////////////////////////////////////////////////////////
  // DISCONNECT
  ////////////////////////////////////////////////////////////

  public void disconnect() {

    Robot robot = Robot.getInstance();
    System.out.println("Communication: Robot "+robot.getId()+" is trying to leave the network.");
    
    // send message to the admin
    ClientResponse response = RESTutils.RESTDeleteRobot(robot.getCityId(), robot.getId());
    if (response.getStatus() != Response.Status.OK.getStatusCode()) {
      System.out.println("Communication: Robot "+robot.getId()+" failed to remove from admin."
          + "\n\tError code: " + response.getStatus());
      throw new RuntimeException("Robot "+robot.getId()+" failed to remove from admin." 
          + "\n\tError code: " + response.getStatus());
    }
    System.out.println("Communication: Robot "+robot.getId()+" removed from admin.");

    //! send messages to the other robots
    Robot.getInstance().getNetwork().leaveNetwork();

    System.out.println("Communication: Robot "+robot.getId()+" left the network.");

  }

}
