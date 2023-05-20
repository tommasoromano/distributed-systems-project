package adminserver;

import adminserver.REST.beans.RobotBean;
import utils.District;
import utils.IRobot;
import utils.Position;

public class RegisteredRobot implements IRobot {
  private int id;
  private String ipAddress;
  private int portNumber;
  private Position startPosition;
  private Position position;
  private District district;
  public RegisteredRobot(int id, String ipAddress, int portNumber, Position startPosition, District district) {
    this.id = id;
    this.ipAddress = ipAddress;
    this.portNumber = portNumber;
    this.startPosition = startPosition;
    this.position = startPosition;
    this.district = district;
  }
  public int getId() {
    return this.id;
  }
  public String getIpAddress() {
    return this.ipAddress;
  }
  public int getPort() {
    return this.portNumber;
  }
  public Position getStartPosition() {
    return this.startPosition;
  }
  public Position getPosition() {
    return this.position;
  }
  public District getDistrict() {
    return this.district;
  }
  public RobotBean createRobot() {
    return new RobotBean(this.id, this.ipAddress, this.portNumber);
  }
  public String getRepresentation() {
    return "Robot: " + this.id
        + "\n\tDistrict: " + this.district.getId()
        + "\n\tPosition:  " + this.position.toString()
        + "\n\tIP address: " + this.ipAddress
        + "\n\tPort: " + this.portNumber;

  }
}
