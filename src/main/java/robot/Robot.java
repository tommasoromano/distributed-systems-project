package robot;

import robot.communication.RobotCommunication;
import utils.City;

//! ROBOT CREATION IN WHICH DISTRICT? ADMIN?

public class Robot {
  private static Robot instance = null;

  private RobotInput input;
  private RobotSensor sensor;
  private RobotCommunication communication;
  private RobotNetwork network;

  private Thread inputThread;
  private Thread sensorThread;
  private Thread communicationThread;
  private Thread networkThread;

  private City city = null;
  private int id = -1;
  private String ipAddress = "";
  private int portNumber = -1;

  private boolean init = false;

  private Robot() {
    this.input = new RobotInput(this);
    this.sensor = new RobotSensor();
    this.communication = new RobotCommunication(this);
    this.network = new RobotNetwork();
  }
  public static Robot getInstance() {
    if (instance == null) {
      instance = new Robot();
    }
    return instance;
  }
  public void start() {
    Thread inputThread = new Thread(this.input);
    // Thread sensorThread = new Thread(this.sensor);
    // Thread communicationThread = new Thread(this.communication);
    // Thread networkThread = new Thread(this.network);

    this.inputThread = inputThread;
    // this.sensorThread = sensorThread;
    // this.communicationThread = communicationThread;
    // this.networkThread = networkThread;

    inputThread.start();
    // sensorThread.start();
    // communicationThread.start();
    // networkThread.start();
  }

  public City getCity() {
    return this.city;
  }
  public void setCity(City city) {
    if (this.city != null) {
      System.out.println("Robot already has a city.");
      return;
    }
    this.city = city;
  }
  public int getId() {
    return this.id;
  }
  public void setId(int id) {
    if (this.id != -1) {
      System.out.println("Robot already has an id.");
      return;
    }
    this.id = id;
  }
  public String getIpAddress() {
    return this.ipAddress;
  }
  public void setIpAddress(String ipAddress) {
    if (!this.ipAddress.equals("")) {
      System.out.println("Robot already has an ip address.");
      return;
    }
    this.ipAddress = ipAddress;
  }
  public int getPortNumber() {
    return this.portNumber;
  }
  public void setPortNumber(int portNumber) {
    if (this.portNumber != -1) {
      System.out.println("Robot already has a port number.");
      return;
    }
    this.portNumber = portNumber;
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

    boolean success = this.communication.joinNetwork();

    if (!success) {
      destroy();
      return;
    }
    this.init = true;
  }

  public void disconnect() {

    // send message to the admin

    this.destroy();
  }

  private void destroy() {
    this.inputThread.interrupt();
    // this.sensorThread.interrupt();
    // this.communicationThread.interrupt();
    // this.networkThread.interrupt();
    this.inputThread = null;
    // this.sensorThread = null;
    // this.communicationThread = null;
    // this.networkThread = null;
    this.city = null;
    this.id = -1;
    this.ipAddress = "";
    this.portNumber = -1;
    this.init = false;

    // remove instance
    instance = null;
  }
}
