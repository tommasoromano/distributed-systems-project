package robot.communication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response;

import adminserver.REST.RESTutils;
import adminserver.REST.beans.InsertRobotBean;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import protos.network.NetworkMessage;
import protos.network.NetworkResponse;
import protos.network.NetworkServiceGrpc;
import protos.network.NetworkServiceGrpc.NetworkServiceStub;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.sun.jersey.api.client.ClientResponse;

import robot.Robot;
import robot.communication.network.GRPCServer;
import utils.City;
import utils.Config;

public class RobotCommunication {

  private MqttClient client;

  private Thread grpcThread;
  private Map<Integer, NetworkServiceStub> grpcStubsMap = new HashMap<>();

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
      return null;
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

  public void sendMessageToRobot(NetworkMessage msg, int recipientId, int recipientPort) {
    this.sendGRPCMessage(msg, recipientId, recipientPort);
  }

  private void sendGRPCMessage(NetworkMessage request, int recipientId, int recipientPort) {
    
    try {

      if (!this.grpcStubsMap.containsKey(recipientPort)) {

      final ManagedChannel _channel = ManagedChannelBuilder.forTarget("localhost:"+recipientPort).usePlaintext().build();
      NetworkServiceStub _stub = NetworkServiceGrpc.newStub(_channel);
        this.grpcStubsMap.put(recipientPort, _stub);
      }

      NetworkServiceStub stub = this.grpcStubsMap.get(recipientPort);

      // System.out.println("gRPC: Sending message: "+request.getMessageType()+" from "+request.getSenderId()+" at "+request.getSenderPort()+" with timestamp "+request.getTimestamp());

      StreamObserver<NetworkResponse> responseObserver = new StreamObserver<NetworkResponse>() {
        @Override
        public void onNext(NetworkResponse response) {
          // System.out.println("gRPC Observer: Response: "+response.getMessageType()+" from "+response.getSenderId()+" at "+response.getSenderPort()+" with timestamp "+response.getTimestamp());
          Robot.getInstance().getNetwork().createResponseForRobotMessage(response);
        }
        @Override
        public void onError(Throwable t) {
          // if no response remove from network
          System.out.println("gRPC Observer: Error: "+t.getMessage());
          try {
            Robot.getInstance().getNetwork().createResponseForRobotMessage(
              NetworkMessage.newBuilder()
                .setMessageType("LEAVING")
                .setSenderId(recipientId)
                .setSenderPort(recipientPort)
                .setTimestamp(System.currentTimeMillis())
                .build()
            );
          } catch (Exception e) {
            System.out.println("gRPC Observer: LEAVING Error: "+e.getMessage());
          }
        }
        @Override
        public void onCompleted() {
          // System.out.println("gRPC Observer: Completed");
        }
      };

      stub.sendNetworkMessage(request, responseObserver);

      //! add channel shutdown when lost connection

    } catch (Exception e) {
      System.out.println("gRPC: Send message Error: "+e.getMessage());
      // e.printStackTrace();
    }
  }

  public NetworkResponse createResponseForRobotMessage(NetworkMessage message) {
    return Robot.getInstance().getNetwork().createResponseForRobotMessage(message);
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

    // mqtt
    try {
      client.disconnect();
      client.close();
    } catch (MqttException e) {
      // e.printStackTrace();
    }
    System.out.println("Communication: Robot "+robot.getId()+" left the MQTT network.");

    // network and grpc
    robot.getNetwork().disconnect();
    for (int port : this.grpcStubsMap.keySet()) {
      ManagedChannel channel = (ManagedChannel) this.grpcStubsMap.get(port).getChannel();
      channel.shutdown();
      // try {
      //   channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
      // } catch (InterruptedException e) {
      //   e.printStackTrace();
      // }
    }
    grpcThread.interrupt();
    System.out.println("Communication: Robot "+robot.getId()+" left the network.");

  }

}
