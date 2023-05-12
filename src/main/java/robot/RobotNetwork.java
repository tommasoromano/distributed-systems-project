package robot;

import java.util.List;

import adminserver.REST.beans.RobotBean;
import utils.Position;

public class RobotNetwork {
  private Position position;
  private List<RobotBean> robots;
  public RobotNetwork(Position position, List<RobotBean> startRobots) {
    this.position = position;
    this.robots = robots;
  }
}
