package robot.network;

import java.util.List;

import adminserver.REST.beans.RobotBean;
import protos.network.NetworkMessage;
import protos.network.NetworkResponse;
import robot.IRobotComponent;
import robot.Robot;
import robot.RobotMaintenance;
import utils.Config;
import utils.Position;

/**
 * [...] If its insertion is successful, the cleaning robot receives
 * from the Administrator Server:
 * • its starting position in one of the smart city districts
 * • the list of the other robots already present in Greenfield (i.e., ID,
 * address, and port number of each robot)
 * Once the cleaning robot receives this information, [...]. 
 * Then, if there are other robots in Greenfield, the
 * cleaning robot presents itself to the other ones by sending them
 * • its position in the grid
 * • its district
 * • its ID
 * • its port number for communications
 * 
 * 
 * [...] The mechanic may be accessed only by a single cleaning robot at a time. 
 * [...], you have to implement
 * one of the distributed algorithms of mutual exclusion introduced in
 * the theory lessons (i.e., Ricart and Agrawala or a ring algorithm) in order
 * to coordinate the maintenance operations of the robots of Greenfield. You
 * have to handle critical issues like the insertion/removal of a robot in the
 * smart city during the execution of the mutual exclusion algorithm.
 * For the sake of simplicity, you can assume that the clocks of the robots
 * are properly synchronized and that the timestamps of their requests will
 * never be the same (like Lamport total order can ensure).
 * [...]
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
public class RobotNetwork implements IRobotComponent {
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

  /**
   * ...if there are other robots in Greenfield, the
   * cleaning robot presents itself to the other ones by sending them:
   * its position in the grid, its district, its ID, its port number for communications
   */
  private void welcomeAll() {
    // log("welcoming all robots.");
    for (RobotBean robot : resource.getRobotsCopy()) {
      log("sending welcome message to "+robot.getId());
      Robot.getInstance().getCommunication()
        .sendMessageToRobot(
          buildNetworkMessage(MessageTypes.WELCOME, createWelcomePayload()),
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

    checkAndRunMaintenance();

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

    // log("received message "+message.getMessageType()+" from "+message.getSenderId()+" at "+message.getSenderPort()+" with timestamp "+message.getTimestamp());

    switch(type) {
      case ACK:
        return ackResponse();
      case WELCOME:
        return this.welcomeNewRobot(message);
      case LEAVING:
        return this.robotLeftNetwork(message);
      case ASK_FOR_MAINTENANCE:
        return this.robotAskedForMaintenence(message);
      case MAINTENANCE_OK:
        return this.robotOkedForMaintenance(message);
      default:
        log("received unknown message type: "+type);
    }

    return builNetworkResponse(MessageTypes.ERROR, null);
  }

  private NetworkResponse welcomeNewRobot(NetworkMessage message) {
    int senderId = message.getSenderId();
    int senderPort = message.getSenderPort();

    if (resource.containsRobot(senderId)) {
      log("Robot "+senderId+" already in network "+resource.getRobotsToString());
      return ackResponse();
    }
    RobotBean newRobot = new RobotBean(senderId, "localhost", senderPort);
    resource.addRobot(newRobot);
    log("Robot "+senderId+" welcomed and added to network "+resource.getRobotsToString());
    return ackResponse();
  }

  private NetworkResponse robotLeftNetwork(NetworkMessage message) {
    int senderId = message.getSenderId();

    //TODO: add remove communication stub?
    if (!resource.containsRobot(senderId)) {
      log("Robot "+senderId+" not in network "+resource.getRobotsToString());
      return ackResponse();
    }
    resource.removeRobot(senderId);
    resource.removeOk(senderId);
    resource.removeQueueNode(senderId);
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
      return ackResponse();
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

  /**
   * ...position in the grid, its district, its ID, its port number for communications
   */
  private String createWelcomePayload() {
    return Robot.getInstance().getPosition().getX() + "," +
      Robot.getInstance().getPosition().getY() + "," +
      Robot.getInstance().getDistrictId() + "," ;
  }

  private void log(String message) {
    System.out.println("Network ["+Robot.getInstance().getId()+"]: "+message);
  }

  public synchronized void destroy() {
    this.leaveNetwork();
    log("Destroyed.");
  }

}
