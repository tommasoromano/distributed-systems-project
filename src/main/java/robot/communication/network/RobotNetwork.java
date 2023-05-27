package robot.communication.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import adminserver.REST.beans.RobotBean;
import protos.network.NetworkMessage;
import protos.network.NetworkResponse;
import robot.Robot;
import robot.communication.RobotMaintenance;
import utils.Position;

/**
 * Every 10 seconds, each cleaning robot has a chance of 10% to be subject
 * to malfunctions. In this case, the cleaning robot must go to the mechanic
 * of Greenfield (for simplicity, it is not necessary to simulate that the robot
 * actually reaches the mechanic in the smart city grid). The mechanic may
 * be accessed only by a single cleaning robot at a time. It is also possible to
 * explicitly ask a cleaning robot to go to the mechanic through a specific command 
 * (i.e., fix ) on the command line. In both cases, you have to implement
 * one of the distributed algorithms of mutual exclusion introduced in the theory 
 * lessons in order to coordinate the maintenance operations of the robots 5
 * of Greenfield. You have to handle critical issues like the insertion/removal
 * of a robot in the smart city during the execution of the mutual exclusion algorithm.
 * For the sake of simplicity, you can assume that the clocks of the robots
 * are properly synchronized and that the timestamps of their requests will
 * never be the same (like Lamport total order can ensure). Note that, all the
 * communications between the robots must be handled through gRPC.
 * The maintenance operation is simulated through a Thread.sleep() of
 * 10 seconds.
 * 
 * @param position
 * @param startRobots
 */
public class RobotNetwork {
  private Position position;
  private List<RobotBean> robots;
  private RobotBean thisRobot;

  private NetworkQueue networkQueue;
  private List<RobotBean> robotOks;

  enum MessageTypes {
    WELCOME,
    LEAVING,

    ASK_FOR_MAINTENANCE,
    MAINTENANCE_OK,

    ALIVE,

    ACK,
    ERROR
  }

  public RobotNetwork(Position position, List<RobotBean> startRobots) {
    this.position = position;
    this.robots = new ArrayList<>(startRobots);
    this.networkQueue = new NetworkQueue();
    this.robotOks = new ArrayList<>();

    this.thisRobot = new RobotBean(
      Robot.getInstance().getId(),
      "localhost",
      Robot.getInstance().getPortNumber()
    );

    this.welcomeAll();
  }

  ////////////////////////////////////////////////////////////
  // SENDING
  ////////////////////////////////////////////////////////////

  private void welcomeAll() {
    log("welcoming all robots.");
    for (RobotBean robot : getRobots()) {
      log("sending welcome message to "+robot.getId());
      Robot.getInstance().getCommunication()
        .sendMessageToRobot(
          buildNetworkMessage(MessageTypes.WELCOME, null),
          robot.getId(),
          robot.getPortNumber()
        );
    }
  }

  private void leaveNetwork() {
    log("sending leave message to all robots.");
    for (RobotBean robot : getRobots()) {
      log("sending leave message to "+robot.getId());
      Robot.getInstance().getCommunication()
        .sendMessageToRobot(
          buildNetworkMessage(MessageTypes.LEAVING, null), 
          robot.getId(),
          robot.getPortNumber()
      );
    }
  }

  public void askForMaintenance() {
    log("asking for maintenance to all");
    for (RobotBean robot : getRobots()) {
      log("sending maintenance request to "+robot.getId());
      Robot.getInstance().getCommunication()
        .sendMessageToRobot(
          buildNetworkMessage(MessageTypes.ASK_FOR_MAINTENANCE, null),
          robot.getId(),
          robot.getPortNumber()
        );
    }
    log("sending maintenance request to "+thisRobot.getId());
      Robot.getInstance().getCommunication()
        .sendMessageToRobot(
          buildNetworkMessage(MessageTypes.ASK_FOR_MAINTENANCE, null),
          thisRobot.getId(),
          thisRobot.getPortNumber()
        );

    // Timer timer = new Timer();
    // timer.schedule(new TimerTask() {
    //     @Override
    //     public void run() {
    //       sendAlive();
    //     }
    // }, 15 * 1000);
  }

  public void hasFinishedMaintenance() {
    log("sending maintenance ok to all in queue ("+getNetworkQueue().size()+")");
    robotOks = new ArrayList<>();
    List<QueueNode> queue = getNetworkQueue().readAndClear();
    for (QueueNode node : queue) {
      RobotBean robot = node.getRobot();
      log("sending maintenance ok to "+robot.getId());
      Robot.getInstance().getCommunication()
        .sendMessageToRobot(
          buildNetworkMessage(MessageTypes.MAINTENANCE_OK, null),
          robot.getId(),
          robot.getPortNumber()
        );
    }
  }

  ////////////////////////////////////////////////////////////
  // RECEIVING
  ////////////////////////////////////////////////////////////

  public NetworkResponse createResponseForRobotMessage(NetworkResponse response) {
    return createResponseForRobotMessage(
      NetworkMessage.newBuilder()
        .setMessageType(response.getMessageType())
        .setSenderId(response.getSenderId())
        .setSenderPort(response.getSenderPort())
        .setTimestamp(response.getTimestamp())
        .setAdditionalPayload(response.getAdditionalPayload())
        .build()
    );
  }

  public NetworkResponse createResponseForRobotMessage(NetworkMessage message) {

    MessageTypes type = MessageTypes.valueOf(message.getMessageType());
    int senderId = message.getSenderId();
    int senderPort = message.getSenderPort();
    long ts = message.getTimestamp();
    String additionalPayload = message.getAdditionalPayload();

    // log("received message "+message.getMessageType()+" from "+message.getSenderId()+" at "+message.getSenderPort()+" with timestamp "+message.getTimestamp());

    switch(type) {
      case ACK:
        return ackResponse();
      case WELCOME:
        return this.welcomeNewRobot(senderId, senderPort);
      case LEAVING:
        return this.robotLeftNetwork(senderId, senderPort);
      case ASK_FOR_MAINTENANCE:
        return this.robotAskedForMaintenence(message);
      case MAINTENANCE_OK:
        return this.robotOkedForMaintenance(message);
      default:
        log("received unknown message type: "+type);
    }

    return builNetworkResponse(MessageTypes.ERROR, null);
  }

  private NetworkResponse welcomeNewRobot(int senderId, int senderPort) {
    RobotBean newRobot = new RobotBean(senderId, "localhost", senderPort);
    addRobot(newRobot);
    log("Robot "+senderId+" welcomed and added to network.");
    return ackResponse();
  }

  private NetworkResponse robotLeftNetwork(int senderId, int senderPort) {
    //! should I remove from queue? remove stub?
    removeRobotIf(senderId);
    log("Robot "+senderId+" removed from network.");
    return ackResponse();
  }

  private NetworkResponse robotAskedForMaintenence(NetworkMessage message) {

    int senderId = message.getSenderId();
    if (senderId == thisRobot.getId()) {
      log("Robot "+senderId+" asked for maintenance.\n\tI asked myself, responding ok.");
      return builNetworkResponse(MessageTypes.MAINTENANCE_OK, null);
    }
    RobotMaintenance.State thisState = Robot.getInstance().getMaintenance().getState();
    
    if (thisState == RobotMaintenance.State.OUT) {

      log("Robot "+senderId+" asked for maintenance.\n\tI am not asking nor using.");
      return builNetworkResponse(MessageTypes.MAINTENANCE_OK, null);
    
    } else if (thisState == RobotMaintenance.State.ASK) {

      long askedTs = Robot.getInstance().getMaintenance().getAskedTimestamp();
      
      if (askedTs > message.getTimestamp()) {
        log("Robot "+senderId+" asked for maintenance.\n\tI asked first, appending to my queue.");
        addToQueue(senderId, message.getTimestamp());
        return ackResponse();
      } else {
        log("Robot "+senderId+" asked for maintenance.\n\tI asked after.");
        return builNetworkResponse(MessageTypes.MAINTENANCE_OK, null);
      }
    } else if (thisState == RobotMaintenance.State.IN) {
        
        log("Robot "+senderId+" asked for maintenance.\n\tI am using.");
        addToQueue(senderId, message.getTimestamp());
        return ackResponse();
    } else {
      log("Robot "+senderId+" asked for maintenance.\n\tI am in an unknown state.");
    }
    return ackResponse();
  }

  private void addToQueue(int senderId, long timestamp) {
    RobotBean robot = getRobotIf(senderId);
    if (robot == null) {
      //! must fix this
      log("CRITIC! Robot "+senderId+" not found in list.");
      return;
    }
    log("Robot "+senderId+" added to maintenance queue.");
    addQueueNode(new QueueNode(robot, timestamp));;
  }

  private NetworkResponse robotOkedForMaintenance(NetworkMessage message) {

    if (isRobotOk(message.getSenderId())) {
      log("Robot "+message.getSenderId()+" already oked for maintenance ("+robotOks.size()+" of "+robots.size()+").");
      return ackResponse();
    }

    if (thisRobot.getId() != message.getSenderId()) {
      RobotBean robot = getRobotIf(message.getSenderId());
      if (robot == null) {
        //! must fix this
        log("CRITIC! Robot "+message.getSenderId()+" not found in list.");
      }

      addRobotOk(robot);
      log("Robot "+message.getSenderId()+" oked for maintenance ("+robotOks.size()+" of "+robots.size()+").");
    }
    if (getRobotOksSize() == robots.size()) {
      // log("All robots oked for maintenance, starting.");
      Robot.getInstance().getMaintenance().maintenanceGranted();
    }

    return ackResponse();
  }


  ////////////////////////////////////////////////////////////
  // SYNC
  ////////////////////////////////////////////////////////////

  private synchronized List<RobotBean> getRobots() {
    return this.robots;
  }
  private synchronized void addRobot(RobotBean robot) {
    this.robots.add(robot);
  }
  private synchronized void removeRobotIf(int id) {
    this.robots.removeIf((robot) -> robot.getId() == id);
  }
  private synchronized RobotBean getRobotIf(int id) {
    return this.robots.stream()
      .filter((robot) -> robot.getId() == id)
      .findFirst()
      .orElse(null);
  }
  private synchronized List<RobotBean> getRobotOks() {
    return this.robotOks;
  }
  private synchronized boolean isRobotOk(int id) {
    return this.robotOks.stream()
      .anyMatch((robot) -> robot.getId() == id);
  }
  private synchronized void addRobotOk(RobotBean robot) {
    this.robotOks.add(robot);
  }
  private synchronized int getRobotOksSize() {
    return this.robotOks.size();
  }
  private synchronized NetworkQueue getNetworkQueue() {
    return this.networkQueue;
  }
  private synchronized void addQueueNode(QueueNode node) {
    this.networkQueue.add(node);
  }

  ////////////////////////////////////////////////////////////
  // UTILS
  ////////////////////////////////////////////////////////////

  private NetworkMessage buildNetworkMessage(
    MessageTypes messageType,
    String additionalPayload
    ) {
    return NetworkMessage.newBuilder()
      .setMessageType(messageType.toString())
      .setSenderId(thisRobot.getId())
      .setSenderPort(thisRobot.getPortNumber())
      .setTimestamp(System.currentTimeMillis())
      .setAdditionalPayload(additionalPayload == null ? "" : additionalPayload)
      .build();
  }

  private NetworkResponse builNetworkResponse(
    MessageTypes messageType,
    String additionalPayload
    ) {
    return NetworkResponse.newBuilder()
      .setMessageType(messageType.toString())
      .setSenderId(thisRobot.getId())
      .setSenderPort(thisRobot.getPortNumber())
      .setTimestamp(System.currentTimeMillis())
      .setAdditionalPayload(additionalPayload == null ? "" : additionalPayload)
      .build();
  }

  private NetworkResponse ackResponse() {
    return builNetworkResponse(MessageTypes.ACK, null);
  }

  private void log(String message) {
    System.out.println("Network ["+thisRobot.getId()+"]: "+message);
  }

  public void disconnect() {
    //! send to all robots that I am leaving
    this.leaveNetwork();
    System.out.println("Network ["+thisRobot.getId()+"]: Destroyed.");
  }

}
