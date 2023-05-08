package adminserver;

import adminserver.REST.beans.Robot;

public class RegisteredRobot {
  private int id;
  private String ipAddress;
  private int port;
  private Position startPosition;
  private District district;
  public RegisteredRobot(int id, String ipAddress, int port, Position startPosition, District district) {
    this.id = id;
    this.ipAddress = ipAddress;
    this.port = port;
    this.startPosition = startPosition;
    this.district = district;
  }
  public int getId() {
    return this.id;
  }
  public String getIpAddress() {
    return this.ipAddress;
  }
  public int getPort() {
    return this.port;
  }
  public Position getStartPosition() {
    return this.startPosition;
  }
  public District getDistrict() {
    return this.district;
  }
  public Robot createRobot() {
    return new Robot(this.id, this.ipAddress, this.port);
  }
}
