package adminserver;

import java.util.ArrayList;
import java.util.List;

public class District {
  private int id;
  private Grid grid;
  private List<RegisteredRobot> registeredRobots;
  public District(int id, Grid grid) {
    this.id = id;
    this.grid = grid;
    this.registeredRobots = new ArrayList<RegisteredRobot>();
  }
  public int getId() {
    return this.id;
  }
  public Grid getGrid() {
    return this.grid;
  }
  public List<RegisteredRobot> getRegisteredRobots() {
    return this.registeredRobots;
  }
  public void removeRobot(int id) {
    for (RegisteredRobot registeredRobot : this.registeredRobots) {
      if (registeredRobot.getId() == id) {
        this.registeredRobots.remove(registeredRobot);
        return;
      }
    }
  }
  public Position getRandomPosition() {
    return this.grid.getRandomPosition();
  }
  public void addRobot(RegisteredRobot robot) {
    for (RegisteredRobot registeredRobot : this.registeredRobots) {
      if (registeredRobot.getId() == robot.getId()) {
        return;
      }
    }
    registeredRobots.add(robot);
  }
}
