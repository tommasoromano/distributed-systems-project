package robot;

import java.util.HashMap;
import java.util.Map;

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

import robot.IRobotComponent;
import robot.Robot;
import robot.network.GRPCServer;
import utils.City;
import utils.Config;


/**
 * [...] Once it is launched, the cleaning robot process must register itself to the
 * system through the Administrator Server. If its insertion is successful (i.e.,
 * there are no other robots with the same ID), the cleaning robot receives
 * from the Administrator Server:
 * • its starting position in one of the smart city districts
 * • the list of the other robots already present in Greenfield (i.e., ID,
 * address, and port number of each robot)
 * Once the cleaning robot receives this information, [...]. 
 * Then, if there are other robots in Greenfield, the
 * cleaning robot presents itself to the other ones [...]
 * Finally, the cleaning robot connects as a publisher to the MQTT topic
 * of its district.
 * 
 * 
 * [...], each cleaning robot has to communicate to the Administrator
 * Server the list of the averages of the air pollution measurements [...].
 * As already anticipated, the communication of the air pollution measurements 
 * must be handled through MQTT. In particular, a cleaning robot
 * that operates in the district i will publish such data on the following MQTT
 * topic: greenfield/pollution/district{i}
 *
 * 
 * [...] Note that, all the
 * communications between the robots must be handled through gRPC.
 * 
 * 
 * Cleaning robots can terminate in a controlled way. [...]. 
 * At the same time, you must handle also those cases
 * in which a robot unexpectedly leaves the system (e.g., for a crash simulated
 * by stopping the robot process).
 * When a robot wants to leave the system in a controlled way, it must
 * follow the next steps:
 * • complete any operation at the mechanic
 * • notify the other robots of Greenfield
 * • request the Administrator Server to leave Greenfield
 * When a robot unexpectedly leaves the system, the other robots must
 * have a mechanism that allows them to detect this event in order to inform
 * the Administrator Server.
 */
public class RobotCommunication implements IRobotComponent {

  private MqttClient client;

  private Thread grpcThread;
  private Map<Integer, NetworkServiceStub> grpcStubsMap = new HashMap<>();

  private boolean connectedToGRPC = false;

  private int sensorMsgCounter = 0;

  public RobotCommunication() {
  }

  public void start() {

    Robot robot = Robot.getInstance();

    System.out.println("Communication: Robot "+robot.getId()+" is trying to join the network.");
    
    // request admin
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
      robot.onFailedToJoinNetwork();
      return;
    }
    System.out.println("Communication: This Robot "+robot.getId()+" registered to admin.");

    try {
      insertRobotBean = response.getEntity(InsertRobotBean.class);
    } catch (Exception e) {
      System.out.println("Communication: InsertRobotBean Error: "+e.getMessage());
      this.disconnectFromAdmin();
      robot.onFailedToJoinNetwork();
      return;
    }
    System.out.println("Communication: Robot "+robot.getId()+" received response from admin, robots: "+insertRobotBean.getRobots().size());


    // create mqtt
    startMQTT();

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

    robot.onJoinedNetwork(insertRobotBean);

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
          if (t.getMessage().contains("UNAVAILABLE")) {
            try {
              onSendGRPCUnavailable(recipientId, recipientPort);
            } catch (Exception e) {
              System.out.println("gRPC Observer: LEAVING Error: "+e.getMessage());
            }
          }
        }
        @Override
        public void onCompleted() {
          // System.out.println("gRPC Observer: Completed");
        }
      };

      stub.sendNetworkMessage(request, responseObserver);

    } catch (Exception e) {
      System.out.println("gRPC: Send message Error: "+e.getMessage());
      // e.printStackTrace();
    }
  }

  private void onSendGRPCUnavailable(int recipientId, int recipientPort) {

    Robot.getInstance().getNetwork().createResponseForRobotMessage(
      NetworkMessage.newBuilder()
        .setMessageType("LEAVING")
        .setSenderId(recipientId)
        .setSenderPort(recipientPort)
        .setTimestamp(System.currentTimeMillis())
        .build()
    );

    System.out.println("Communication: removing Robot "+recipientId+" from admin...");
    ClientResponse response = RESTutils.RESTDeleteRobot(Robot.getInstance().getCityId(), recipientId);
    // if (response.getStatus() != Response.Status.OK.getStatusCode()) {
    //   System.out.println("Communication: Robot "+recipientId+" failed to remove from admin."
    //       + "\n\tError code: " + response.getStatus());
    // }
  }

  public NetworkResponse createResponseForRobotMessage(NetworkMessage message) {
    return Robot.getInstance().getNetwork().createResponseForRobotMessage(message);
  }

  ////////////////////////////////////////////////////////////
  // DISCONNECT
  ////////////////////////////////////////////////////////////

  public void destroy() {
    Robot robot = Robot.getInstance();
    System.out.println("Communication: Robot "+robot.getId()+" is disconnecting...");
    disconnectFromAdmin();
    disconnectFromMQTT();
    robot.getNetwork().destroy();
    disconnectFromGRPC();
    System.out.println("Communication: Robot "+robot.getId()+" disconnected.");
  }

  private void disconnectFromAdmin() {
    Robot robot = Robot.getInstance();
    System.out.println("Communication: removing from admin...");
    ClientResponse response = RESTutils.RESTDeleteRobot(robot.getCityId(), robot.getId());
    if (response.getStatus() != Response.Status.OK.getStatusCode()) {
      System.out.println("Communication: Robot "+robot.getId()+" failed to remove from admin."
          + "\n\tError code: " + response.getStatus());
      throw new RuntimeException("Robot "+robot.getId()+" failed to remove from admin." 
          + "\n\tError code: " + response.getStatus());
    }
    // System.out.println("Communication: Robot "+robot.getId()+" removed from admin.");
  }

  private void disconnectFromMQTT() {
    System.out.println("Communication: closing MQTT...");
    try {
      client.disconnect();
      client.close();
    } catch (MqttException e) {
      // e.printStackTrace();
    }
    // System.out.println("Communication: Robot "+robot.getId()+" closed MQTT.");
  }

  private void disconnectFromGRPC() {
    System.out.println("Communication: closing GRPC...");
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
    // System.out.println("Communication: closed GRPC.");
  }
}
