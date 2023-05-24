package robot.communication;

import java.util.ArrayList;
import java.util.List;

import adminserver.REST.beans.RobotBean;

public class MaintenanceQueue {
  private List<RobotBean> queue;
  public MaintenanceQueue() {
    this.queue = new ArrayList<>();
  }
  public synchronized void add(RobotBean robot) {
    this.queue.add(robot);
  }
  public synchronized void remove(int robotId) {
    for (RobotBean robot : this.queue) {
      if (robot.getId() == robotId) {
        this.queue.remove(robot);
        return;
      }
    }
  }
  public synchronized RobotBean pop() {
    if (this.queue.isEmpty()) {
      return null;
    }
    RobotBean robot = this.queue.get(0);
    this.queue.remove(0);
    return robot;
  }
  public synchronized RobotBean peek() {
    if (this.queue.isEmpty()) {
      return null;
    }
    return this.queue.get(0);
  }
  public synchronized boolean isEmpty() {
    return this.queue.isEmpty();
  }
  public synchronized boolean contains(int robotId) {
    for (RobotBean robot : this.queue) {
      if (robot.getId() == robotId) {
        return true;
      }
    }
    return false;
  }
}
