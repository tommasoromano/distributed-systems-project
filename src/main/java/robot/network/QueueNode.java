package robot.network;

import adminserver.REST.beans.RobotBean;

public class QueueNode {
  private RobotBean robot;
  private long timestamp;
  public QueueNode(RobotBean robot, long timestamp) {
    this.robot = robot;
    this.timestamp = timestamp;
  }
  public RobotBean getRobot() {
    return this.robot;
  }
  public long getTimestamp() {
    return this.timestamp;
  }
}
