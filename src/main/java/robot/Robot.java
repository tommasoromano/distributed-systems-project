package robot;

import adminserver.REST.beans.InsertRobotBean;
import robot.network.RobotNetwork;
import utils.City;
import utils.Position;

/**
 * Each Cleaning robot is simulated through a process, which is responsible for:
 * • periodically sending the air pollution measurements it collected to the
 * Administrator Server through MQTT
 * • coordinating with the other robots by using gRPC to distributively
 * decide which robot is allowed to reach the mechanic of Greenfield for
 * maintenance issues
 */
public class Robot {
  private static Robot instance = null;

  private RobotInput input;
  private RobotSensor sensor;
  private RobotCommunication communication;
  private RobotNetwork network;
  private RobotMaintenance maintenance;

  private int cityId = -1;
  private int districtId = -1;
  private int id = -1;
  private String ipAddress = "";
  private int portNumber = -1;
  private Position position = null;

  private boolean init = false;

  private Robot() {
    this.input = new RobotInput();
    this.sensor = new RobotSensor();
    this.communication = new RobotCommunication();
    this.maintenance = new RobotMaintenance();
  }
  public static Robot getInstance() {
    if (instance == null) {
      instance = new Robot();
    }
    return instance;
  }

  ////////////////////////////////////////////////////////////
  // INITIALIZATION
  //
  //   A cleaning robot is initialized by specifying
  // • ID
  // • listening port for communications with the other robots
  // • Administrator Server’s address
  // Once it is launched, the cleaning robot process must register itself to the
  // system through the Administrator Server. If its insertion is successful (i.e.,
  // there are no other robots with the same ID), the cleaning robot receives
  // from the Administrator Server:
  // • its starting position in one of the smart city districts
  // • the list of the other robots already present in Greenfield (i.e., ID,
  // address, and port number of each robot)
  // Once the cleaning robot receives this information, it starts acquiring data
  // from its pollution sensor. Then, if there are other robots in Greenfield, the
  // cleaning robot presents itself to the other ones by sending them
  // • its position in the grid
  // • its district
  // • its ID
  // • its port number for communications
  // Finally, the cleaning robot connects as a publisher to the MQTT topic
  // of its district.
  ////////////////////////////////////////////////////////////

  public void start() {

    if (init) { return; }

    this.input.start();
  }

  public void init(int id, String ipAddress, int portNumber) {

    if (init) { return; }

    System.out.println("Initializating robot with:"
    +"\n\tid:          " + id
    +"\n\tip address:  " + ipAddress
    +"\n\tport number: " + portNumber);

    this.setId(id);
    this.setIpAddress(ipAddress);
    this.setPortNumber(portNumber);

    this.communication.start();
  }

  public boolean isInit() {
    return this.init;
  }

  public void onJoinedNetwork(InsertRobotBean insertRobotBean) {

    this.position = new Position(insertRobotBean.getX(), insertRobotBean.getY());
    this.setDistrictId(City.getCityById(this.cityId)
        .getDistrictByPosition(this.position).getId());

    this.network = new RobotNetwork(this.position, insertRobotBean.getRobots());
    this.network.start();

    // sensor.start();
    // communication.start();
    // network.start();
    this.sensor.start();
    this.maintenance.start();

    this.init = true;
    System.out.println("Robot initialized.");
  }

  public void onFailedToJoinNetwork() {
    System.out.println("Robot "+this.id+" failed to join the network.");
    this.ipAddress = "";
    this.portNumber = -1;
    this.init = false;
    this.position = null;
    this.districtId = -1;
  }

  ////////////////////////////////////////////////////////////
  // MISC
  ////////////////////////////////////////////////////////////

  

  ////////////////////////////////////////////////////////////
  // 
  //  DISCONNECT
  // 
  // Cleaning robots can terminate in a controlled way. Specifically, only when
  // the message ”quit” is inserted into the command line of a robot process, it
  // will leave the system. At the same time, you must handle also those cases
  // in which a robot unexpectedly leaves the system (e.g., for a crash simulated
  // by stopping the robot process).
  // When a robot wants to leave the system in a controlled way, it must
  // follow the next steps:
  // • complete any operation at the mechanic
  // • notify the other robots of Greenfield
  // • request the Administrator Server to leave the smart city
  // When a robot unexpectedly leaves the system, the other robots must
  // have a mechanism that allows them to detect this event in order to inform
  // the Administrator Server.
  ////////////////////////////////////////////////////////////

  public void disconnect() {

    if (getMaintenance().getState() == RobotMaintenance.State.IN) {
      System.out.println("Robot "+this.id+" is in maintenance. Waiting for it to finish...");
      // while (getMaintenance().getState() == RobotMaintenance.State.IN) {
      //   try {
      //     Thread.sleep(1000);
      //   } catch (InterruptedException e) {
      //     System.out.println("Robot "+this.id+" was interrupted while waiting for maintenance to finish.");
      //     break;
      //   }
      // }
      getMaintenance().setDisconnectAtMaintenanceEnd();
      return;
    }

    if (Robot.instance == null
    || this.id == -1) { return; }

    System.out.println("Robot "+this.id+" is disconnecting from the network.");

    try {
      this.communication.destroy();
      // this.network.disconnect();
    } catch (Exception e) {
      System.out.println("Robot "+this.id+" failed to disconnect from the network."
        +"\n\t"+e.getMessage());
    }

    this.destroy();
  }

  /**
   * destroys the robot instance and process
   */
  private void destroy() {

    if (Robot.instance == null
    || this.id == -1) { return; }

    System.out.println("Destroying robot "+this.id+".");

    this.input.destroy();
    this.sensor.destroy();
    // this.communication.destroy();
    // this.network.destroy();
    this.maintenance.destroy();

    this.cityId = -1;
    this.id = -1;
    this.ipAddress = "";
    this.portNumber = -1;
    this.init = false;

    // remove instance
    instance = null;

    System.out.println("Robot destroyed.");
    System.out.println("Exiting...");

    Runtime.getRuntime().exit(0);
  }

  ////////////////////////////////////////////////////////////
  // GETTERS AND SETTERS
  ////////////////////////////////////////////////////////////

  public int getCityId() {
    return this.cityId;
  }
  public void setCityId(int cityId) {
    if (this.cityId != -1) {
      System.out.println("Robot already has a cityId.");
      return;
    }
    this.cityId = cityId;
  }
  public int getDistrictId() {
    return this.districtId;
  }
  public void setDistrictId(int districtId) {
    if (this.districtId != -1) {
      System.out.println("Robot already has a districtId.");
      return;
    }
    this.districtId = districtId;
  }
  public Position getPosition() {
    return this.position;
  }
  public int getId() {
    return this.id;
  }
  public void setId(int id) {
    if (this.id != -1 && this.init) {
      System.out.println("Robot already has an id.");
      return;
    }
    this.id = id;
  }
  public String getIpAddress() {
    return this.ipAddress;
  }
  public void setIpAddress(String ipAddress) {
    if (!this.ipAddress.equals("") && this.init) {
      System.out.println("Robot already has an ip address.");
      return;
    }
    this.ipAddress = ipAddress;
  }
  public int getPortNumber() {
    return this.portNumber;
  }
  public void setPortNumber(int portNumber) {
    if (this.portNumber != -1 && this.init) {
      System.out.println("Robot already has a port number.");
      return;
    }
    this.portNumber = portNumber;
  }
  public RobotCommunication getCommunication() {
    return this.communication;
  }
  public RobotNetwork getNetwork() {
    return this.network;
  }
  public RobotMaintenance getMaintenance() {
    return this.maintenance;
  }
}
