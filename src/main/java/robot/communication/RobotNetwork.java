package robot.communication;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import adminserver.REST.beans.RobotBean;
import protos.network.Network;
import protos.network.NetworkMessage;
import protos.network.NetworkResponse;
import robot.Robot;
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
  private RobotBean coordinator;
  private boolean isElectionStarted = false;
  private MaintenanceQueue maintenanceQueue;

  enum MessageTypes {
    WELCOME,
    WELCOME_RESPONSE,
    LEAVING,
    LEAVING_RESPONSE,

    ASK_FOR_MAINTENANCE,
    MAINTENANCE_GRANTED,
    MAINTENANCE_NOT_GRANTED,
    MAINTENENCE_FINISHED,
    MAINTENENCE_FINISHED_RESPONSE,

    ASK_FOR_ELECTION,
    ELECTION_OK,
    ELECTION_NOT_OK,
    COORDINATOR,
    COORDINATOR_RESPONSE,

    ERROR
  }

  public RobotNetwork(Position position, List<RobotBean> startRobots) {
    this.position = position;
    this.robots = new ArrayList<>(startRobots);
    this.maintenanceQueue = new MaintenanceQueue();

    this.thisRobot = new RobotBean(
      Robot.getInstance().getId(),
      "localhost",
      Robot.getInstance().getPortNumber()
    );
    this.coordinator = this.thisRobot;
    for (RobotBean robot : startRobots) {
      if (robot.getId() > this.coordinator.getId()) {
        this.coordinator = robot;
      }
    }

    this.welcomeAll();
  }

  ////////////////////////////////////////////////////////////
  // SENDING
  ////////////////////////////////////////////////////////////

  private void welcomeAll() {
    log("welcoming all robots.");
    for (RobotBean robot : this.robots) {
      NetworkResponse response = Robot.getInstance().getCommunication()
        .sendMessageToRobot(
          buildNetworkMessage(MessageTypes.WELCOME, null),
          robot.getPortNumber()
        );
      
      if (response == null) {
        handleNoResponse(robot.getId(), false);
      }
    }
    // log("Robot "+thisRobot.getId()+" welcomed all robots.");
  }

  public void leaveNetwork() {
    log("sending leave message to all robots.");
    for (RobotBean robot : this.robots) {
      Robot.getInstance().getCommunication()
        .sendMessageToRobot(
          buildNetworkMessage(MessageTypes.LEAVING, null), 
          robot.getPortNumber()
      );
    }
    //! if I am coordinator, should I propagate the maintenence queue?
  }

  public void askForMaintenance() {

    NetworkResponse response = Robot.getInstance().getCommunication()
      .sendMessageToRobot(
        buildNetworkMessage(MessageTypes.ASK_FOR_MAINTENANCE, null), 
        this.coordinator.getPortNumber()
      );

    // log("asked for maintenance to "+this.coordinator.getId()
    //     +"\n\tresponse "+response);
    
    if (response == null) {
      handleNoResponse(this.coordinator.getId(), true);
      return;
    }

    MessageTypes responseType = MessageTypes.valueOf(response.getMessageType());

    switch(responseType) {
      case MAINTENANCE_GRANTED:
        log("maintenance granted by "+this.coordinator.getId());
        //! call this robot to start maintenence
        Robot.getInstance().getMaintenance().maintenanceGranted();
        break;
      case MAINTENANCE_NOT_GRANTED:
        log("maintenance not granted by "+this.coordinator.getId());
        //! have to wait?
        Robot.getInstance().getMaintenance().maintenanceNotGranted();
        break;
      default:
        log("received unexpected response from "+this.coordinator.getId());
        break;
    }

  }

  public void hasFinishedMaintenance() {

    NetworkResponse response = Robot.getInstance().getCommunication()
      .sendMessageToRobot(
        buildNetworkMessage(MessageTypes.MAINTENENCE_FINISHED, null), 
        this.coordinator.getPortNumber()
    );

    if (response == null) {
      handleNoResponse(this.coordinator.getId(), true);
      return;
    }

  }

  private void handleNoResponse(int robotId, boolean startElection) {
    if (robotId == this.coordinator.getId() && startElection) {
      log("coordinator did not respond, starting election.");
      this.askToStartElection();
    } else {
      log("robot "+robotId+" did not respond, removing from network.");
      this.robots.removeIf((robot) -> robot.getId() == robotId);
    }
  }

  private void askToStartElection() {

    if (isElectionStarted) {
      log("election already started (asking).");
      return;
    }
    isElectionStarted = true;

    log("Robot "+thisRobot.getId()+" asking to start election.");

    this.coordinator = thisRobot;
    for (RobotBean robot : this.robots) {
      if (robot.getId() > this.thisRobot.getId()) {
        NetworkResponse response = Robot.getInstance().getCommunication()
          .sendMessageToRobot(
            buildNetworkMessage(MessageTypes.ASK_FOR_ELECTION, null), 
            robot.getPortNumber()
          );
        if (response == null) {
          handleNoResponse(robot.getId(), false);
          continue;
        }
        MessageTypes responseType = MessageTypes.valueOf(response.getMessageType());
        switch(responseType) {
          case ELECTION_OK:
            this.coordinator = null;
            log("Robot "+robot.getId()+" accepted election, means I am not coordinator.");
            break;
          case ELECTION_NOT_OK:
            log("Robot "+robot.getId()+" did not accept election.");
            break;
          default:
            log("Robot "+robot.getId()+" sent unexpected response.");
            break;
        }
      }
    }

    if (this.coordinator != null && this.coordinator.getId() == thisRobot.getId()) {
      log("I am the new coordinator.");
      for (RobotBean robot : this.robots) {
        NetworkResponse response = Robot.getInstance().getCommunication()
          .sendMessageToRobot(
            buildNetworkMessage(MessageTypes.COORDINATOR, null), 
            robot.getPortNumber()
          );
        if (response == null) {
          handleNoResponse(robot.getId(), false);
          continue;
        }
      }
      this.isElectionStarted = false;
    }
  }


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

  ////////////////////////////////////////////////////////////
  // RECEIVING
  ////////////////////////////////////////////////////////////

  public NetworkResponse createResponseForRobotMessage(NetworkMessage message) {

    MessageTypes type = MessageTypes.valueOf(message.getMessageType());
    int senderId = message.getSenderId();
    int senderPort = message.getSenderPort();
    long ts = message.getTimestamp();
    String additionalPayload = message.getAdditionalPayload();

    log("received message "+message.getMessageType()+" from "+message.getSenderId()+" at "+message.getSenderPort()+" with timestamp "+message.getTimestamp());

    //! make a msg queue with synchronized method?

    switch(type) {
      case WELCOME:
        return this.welcomeNewRobot(senderId, senderPort);
      case LEAVING:
        return this.robotLeftNetwork(senderId, senderPort);
      case ASK_FOR_MAINTENANCE:
        return this.robotAskedForMaintenence(senderId, senderPort);
      case MAINTENANCE_GRANTED:
        //! before there were robots but 
        //! coordinator said that now the queue is empty
        break;
      case MAINTENENCE_FINISHED:
        return this.robotFinishedMaintenence(senderId, senderPort);
      case ASK_FOR_ELECTION:
        return this.robotAskedToStartElection(senderId, senderPort);
      case COORDINATOR:
        return this.robotIsCoordinator(senderId, senderPort);
      default:
        log("received unknown message type: "+type);
    }

    return builNetworkResponse(MessageTypes.ERROR, null);
  }

  private NetworkResponse welcomeNewRobot(int senderId, int senderPort) {
    RobotBean newRobot = new RobotBean(senderId, "localhost", senderPort);
    // add robot to list
    this.robots.add(newRobot);
    log("Robot "+senderId+" welcomed and added to network.");

    // check if new robot is coordinator
    if (senderId > this.coordinator.getId()) {
      this.coordinator = newRobot;
      log("Robot "+senderId+" is new coordinator.");
    }

    return builNetworkResponse(MessageTypes.WELCOME_RESPONSE, null);
  }

  private NetworkResponse robotLeftNetwork(int senderId, int senderPort) {

    // remove robot from list
    this.robots.removeIf((robot) -> robot.getId() == senderId);
    log("Robot "+senderId+" removed from network.");

    // check if coordinator left
    if (senderId == this.coordinator.getId()) {
      this.coordinator = this.robots.stream()
        .max(Comparator.comparing(RobotBean::getId))
        .orElse(this.thisRobot);
      log("Robot "+senderId+" was coordinator, new coordinator is "+this.coordinator.getId());
      //! start election ?
    }

    return builNetworkResponse(MessageTypes.LEAVING_RESPONSE, null);
  }

  private NetworkResponse robotAskedForMaintenence(int senderId, int senderPort) {
    
    if (this.coordinator.getId() != this.thisRobot.getId()) {
      //! add different message? should i inform my coordinator?
      log("Robot "+senderId+" asked for maintenance, but I'm not the coordinator.");
      return builNetworkResponse(MessageTypes.MAINTENANCE_NOT_GRANTED, null);
    }

    RobotBean robot = this.robots.stream()
      .filter((r) -> r.getId() == senderId)
      .findFirst()
      .orElse(null);
    
    if (thisRobot.getId() == senderId) {
      robot = thisRobot;
      log("Robot "+senderId+" asked for maintenance, and it's me.");
    }

    if (robot == null) {
      //! should I add the robot in the network?
      log("Robot "+senderId+" asked for maintenance, but it's not in the network.");
      return builNetworkResponse(MessageTypes.MAINTENANCE_NOT_GRANTED, null);
    }

    if (this.maintenanceQueue.contains(senderId)) {
      //! maybe add different response messages?
      log("Robot "+senderId+" asked for maintenance, but it's already in the queue.");
      return builNetworkResponse(MessageTypes.MAINTENANCE_NOT_GRANTED, null);
    }

    if (this.maintenanceQueue.isEmpty()) {
      log("Robot "+senderId+" asked for maintenance, granting.");
      this.maintenanceQueue.add(robot);
      return builNetworkResponse(MessageTypes.MAINTENANCE_GRANTED, null);
    } 

    this.maintenanceQueue.add(robot);
    log("Robot "+senderId+" asked for maintenance, but there are other robots in the queue.");
    return builNetworkResponse(MessageTypes.MAINTENANCE_NOT_GRANTED, null);

  }

  private NetworkResponse robotFinishedMaintenence(int senderId, int senderPort) {
    
    if (this.coordinator.getId() != this.thisRobot.getId()) {
      //! add different message? should i inform my coordinator?
      log("Robot "+senderId+" finished maintenance, but I'm not the coordinator.");
      return builNetworkResponse(MessageTypes.MAINTENENCE_FINISHED_RESPONSE, null);
    }

    RobotBean robot = this.robots.stream()
      .filter((r) -> r.getId() == senderId)
      .findFirst()
      .orElse(null);
    
    if (thisRobot.getId() == senderId) {
      robot = thisRobot;
      log("Robot "+senderId+" finished maintenance, and it's me.");
    }

    if (robot == null) {
      //! should I add the robot in the network?
      log("Robot "+senderId+" finished maintenance, but it's not in the network.");
      return builNetworkResponse(MessageTypes.MAINTENENCE_FINISHED_RESPONSE, null);
    }

    if (this.maintenanceQueue.isEmpty()) {
      //! maybe add different response messages?
      log("Robot "+senderId+" finished maintenance, but there are no robots in the queue.");
      return builNetworkResponse(MessageTypes.MAINTENENCE_FINISHED_RESPONSE, null);
    }

    this.maintenanceQueue.remove(senderPort);
    log("Robot "+senderId+" finished maintenance, removing from queue.");

    RobotBean nextRobot = this.maintenanceQueue.peek();
    if (nextRobot != null) {
      //! should I send a message to the coordinator of the next robot?
    }

    return builNetworkResponse(MessageTypes.MAINTENENCE_FINISHED_RESPONSE, null);

  }

  private NetworkResponse robotAskedToStartElection(int senderId, int senderPort) {

    MessageTypes responseType = senderId < thisRobot.getId() ? MessageTypes.ELECTION_OK : MessageTypes.ELECTION_NOT_OK;
    
    if (this.isElectionStarted) {
      log("election already started (receiving).");
      return builNetworkResponse(responseType, null);
    }

    // propagate election
    log("propagate election.");
    this.askToStartElection();

    return builNetworkResponse(responseType, null);
  }

  private NetworkResponse robotIsCoordinator(int senderId, int senderPort) {

    this.isElectionStarted = false;
    
    // set new coordinator
    this.coordinator = this.robots.stream()
      .filter((r) -> r.getId() == senderId)
      .findFirst()
      .orElse(null);
    
    if (this.coordinator == null) {
      log("Robot "+senderId+" is new coordinator, but it's not in the network.");
      //! must fix this
      return builNetworkResponse(MessageTypes.COORDINATOR_RESPONSE, null);
    }

    log("Robot "+senderId+" is new coordinator.");
    return builNetworkResponse(MessageTypes.COORDINATOR_RESPONSE, null);

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

  ////////////////////////////////////////////////////////////
  // UTILS
  ////////////////////////////////////////////////////////////

  private void log(String message) {
    System.out.println("Network ["+thisRobot.getId()+"]: "+message);
  }

}
