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
import utils.Config;
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
 * are properly and that the timestamps of their requests will
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
  private RobotNetworkResources resource;
  // private Timer robotAskRetrier;
  private Thread retryAskThread;

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
    this.resource = new RobotNetworkResources(startRobots);
  }

  ////////////////////////////////////////////////////////////
  // SENDING
  ////////////////////////////////////////////////////////////

  public void start() {
    log("starting network");
    welcomeAll();
  }

  private void welcomeAll() {
    // log("welcoming all robots.");
    for (RobotBean robot : resource.getRobotsCopy()) {
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
    // log("sending leave message to all robots.");
    for (RobotBean robot : resource.getRobotsCopy()) {
      log("sending leave message to "+robot.getId());
      Robot.getInstance().getCommunication()
        .sendMessageToRobot(
          buildNetworkMessage(MessageTypes.LEAVING, null), 
          robot.getId(),
          robot.getPortNumber()
      );
    }
  }

  public synchronized void askForMaintenance() {

    if (Robot.getInstance().getMaintenance().getState() != RobotMaintenance.State.ASK) {
      return;
    }

    checkAndRunMaintenance();

    // log("asking for maintenance to all");
    for (RobotBean robot : resource.getRobotsCopy()) {
      // check if have already sent
      if (resource.containsOk(robot.getId())) {
        continue;
      }
      log("sending maintenance request to "+robot.getId());
      Robot.getInstance().getCommunication()
        .sendMessageToRobot(
          buildNetworkMessage(MessageTypes.ASK_FOR_MAINTENANCE, null),
          robot.getId(),
          robot.getPortNumber()
        );
    }

    retryAskThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Thread.sleep(Config.MALFUNCTION_RETRY_ASK_AFTER * 1000);
          log("retrying ask for maintenance");
          askForMaintenance();
        } catch (InterruptedException e) {
          // e.printStackTrace();
        }
      }
    });
    retryAskThread.start();
  }

  public synchronized void hasFinishedMaintenance() {
    log("sending maintenance ok to all in queue "+resource.getQueueToString()+"");
    resource.clearOk();
    List<QueueNode> queue = resource.readAndClearQueue();
    for (QueueNode node : queue) {
      RobotBean robot = node.getRobot();
      // log("sending maintenance ok to "+robot.getId());
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

  public synchronized NetworkResponse createResponseForRobotMessage(NetworkMessage message) {

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
    if (resource.containsRobot(senderId)) {
      log("Robot "+senderId+" already in network "+resource.getRobotsToString());
      return ackResponse();
    }
    RobotBean newRobot = new RobotBean(senderId, "localhost", senderPort);
    resource.addRobot(newRobot);
    log("Robot "+senderId+" welcomed and added to network "+resource.getRobotsToString());
    return ackResponse();
  }

  private NetworkResponse robotLeftNetwork(int senderId, int senderPort) {
    //! should I remove from queue? remove stub?
    if (!resource.containsRobot(senderId)) {
      log("Robot "+senderId+" not in network "+resource.getRobotsToString());
      return ackResponse();
    }
    resource.removeRobot(senderId);
    log("Robot "+senderId+" removed from network "+resource.getRobotsToString());
    
    checkAndRunMaintenance();

    return ackResponse();
  }

  private NetworkResponse robotAskedForMaintenence(NetworkMessage message) {

    int senderId = message.getSenderId();

    if (senderId == Robot.getInstance().getId()) {
      log("Robot "+senderId+" asked for maintenance.\n\tI asked myself, responding ok.");
      return builNetworkResponse(MessageTypes.MAINTENANCE_OK, null);
    }

    RobotMaintenance.State thisState = Robot.getInstance().getMaintenance().getState();
    if (thisState == RobotMaintenance.State.OUT) {
      log("Robot "+senderId+" asked for maintenance.\n\tI am nor asking nor using, responding ok.");
      return builNetworkResponse(MessageTypes.MAINTENANCE_OK, null);
    } else if (thisState == RobotMaintenance.State.ASK) {
      long thisAskedTs = Robot.getInstance().getMaintenance().getAskedTimestamp();
      if (thisAskedTs < message.getTimestamp()) {
        addToQueue(senderId, message.getTimestamp());
        log("Robot "+senderId+" asked for maintenance.\n\tI asked berfore, appending to my queue "+resource.getQueueToString());
        return ackResponse();
      } else {
        log("Robot "+senderId+" asked for maintenance.\n\tI asked after, responding ok.");
        return builNetworkResponse(MessageTypes.MAINTENANCE_OK, null);
      }
    } else if (thisState == RobotMaintenance.State.IN) {
      addToQueue(senderId, message.getTimestamp());
      log("Robot "+senderId+" asked for maintenance.\n\tI am using, appending to my queue "+resource.getQueueToString());
      return ackResponse();
    } else {
      log("Robot "+senderId+" asked for maintenance.\n\tI am in an unknown state.");
    }
    return ackResponse();
  }

  private void addToQueue(int senderId, long timestamp) {
    if (resource.containsQueue(senderId)) {
      // log("Robot "+senderId+" already in queue. "+resource.getQueueToString());
      return;
    }
    RobotBean robot = resource.getRobot(senderId);
    if (robot == null) {
      //! must fix this
      log("CRITIC! Robot "+senderId+" not found in list.");
      return;
    }
    resource.addQueueNode(new QueueNode(robot, timestamp));;
    // log("Robot "+senderId+" added to queue."+resource.getQueueToString());
  }

  private NetworkResponse robotOkedForMaintenance(NetworkMessage message) {

    if (Robot.getInstance().getMaintenance().getState() != RobotMaintenance.State.ASK) {
      log("Robot "+message.getSenderId()+" ok, but I am not asking.");
      return ackResponse();
    }

    if (resource.containsOk(message.getSenderId())) {
      log("Robot "+message.getSenderId()+" already ok " + "( " +resource.getOksToString()+ " of "+resource.getRobotsSize()+ " )");
      checkAndRunMaintenance();
      return ackResponse();
    }

    RobotBean robot = resource.getRobot(message.getSenderId());
    if (robot == null) {
      //! must fix this
      log("CRITIC! Robot "+message.getSenderId()+" not found in list.");
    }
    resource.addOk(robot);
    log("Robot "+message.getSenderId()+" ok " + "( " +resource.getOksToString()+ " of "+resource.getRobotsSize()+ " )");

    checkAndRunMaintenance();

    return ackResponse();
  }

  private void checkAndRunMaintenance() {
    if (Robot.getInstance().getMaintenance().getState() != RobotMaintenance.State.ASK) {
      return;
    }
    if (resource.getOksSize() >= resource.getRobotsSize()) {
      // log("All robots oked for maintenance, starting.");
      retryAskThread.interrupt();
      Robot.getInstance().getMaintenance().maintenanceGranted();
    }
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
      .setSenderId(Robot.getInstance().getId())
      .setSenderPort(Robot.getInstance().getPortNumber())
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
      .setSenderId(Robot.getInstance().getId())
      .setSenderPort(Robot.getInstance().getPortNumber())
      .setTimestamp(System.currentTimeMillis())
      .setAdditionalPayload(additionalPayload == null ? "" : additionalPayload)
      .build();
  }

  private NetworkResponse ackResponse() {
    return builNetworkResponse(MessageTypes.ACK, null);
  }

  private void log(String message) {
    System.out.println("Network ["+Robot.getInstance().getId()+"]: "+message);
  }

  public synchronized void disconnect() {
    //! send to all robots that I am leaving
    this.leaveNetwork();
    System.out.println("Network ["+Robot.getInstance().getId()+"]: Destroyed.");
  }

}
