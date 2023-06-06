package robot.network;

import java.util.ArrayList;
import java.util.List;

import adminserver.REST.beans.RobotBean;
import robot.Robot;

public class RobotNetworkResources {

  private List<RobotBean> robots;
  private NetworkQueue networkQueue;
  private List<RobotBean> robotOks;

  public RobotNetworkResources(List<RobotBean> startRobots) {
    robots = new ArrayList<>(startRobots);
    networkQueue = new NetworkQueue();
    robotOks = new ArrayList<>();
    this.robots.add(new RobotBean(Robot.getInstance().getId(),"localhost",Robot.getInstance().getPortNumber()));
  }

  ////////////////////////////////////////////////////////////
  // ROBOTS
  ////////////////////////////////////////////////////////////

  public synchronized List<RobotBean> getRobotsCopy() {
    return new ArrayList<>(this.robots);
  }
  public synchronized void addRobot(RobotBean robot) {
    this.robots.add(robot);
  }
  public synchronized void removeRobot(int id) {
    this.robots.removeIf((robot) -> robot.getId() == id);
  }
  public synchronized boolean containsRobot(int id) {
    return this.robots.stream()
      .anyMatch((robot) -> robot.getId() == id);
  }
  public synchronized RobotBean getRobot(int id) {
    return this.robots.stream()
      .filter((robot) -> robot.getId() == id)
      .findFirst()
      .orElse(null);
  }
  public synchronized int getRobotsSize() {
    return this.robots.size();
  }
  public synchronized String getRobotsToString() {
    String s = "[ ";
    for (RobotBean robot : this.robots) {
      s += robot.getId() + " ";
    }
    return s + "]";
  }

  ////////////////////////////////////////////////////////////
  // OKS
  ////////////////////////////////////////////////////////////
  
  public synchronized void clearOk() {
    this.robotOks.clear();
  }
  public synchronized boolean containsOk(int id) {
    return this.robotOks.stream()
      .anyMatch((robot) -> robot.getId() == id);
  }
  public synchronized void addOk(RobotBean robot) {
    this.robotOks.add(robot);
  }
  public synchronized void removeOk(int id) {
    this.robotOks.removeIf((robot) -> robot.getId() == id);
  }
  public synchronized int getOksSize() {
    return this.robotOks.size();
  }
  public synchronized String getOksToString() {
    String s = "[ ";
    for (RobotBean robot : this.robotOks) {
      s += robot.getId() + " ";
    }
    return s + "]";
  }

  ////////////////////////////////////////////////////////////
  // QUEUE
  ////////////////////////////////////////////////////////////

  public synchronized int getQueueSize() {
    return this.networkQueue.size();
  }
  public synchronized boolean containsQueue(int id) {
    return this.networkQueue.contains(id);
  }
  public synchronized void addQueueNode(QueueNode node) {
    this.networkQueue.add(node);
  }
  public synchronized void removeQueueNode(int id) {
    this.networkQueue.remove(id);
  }
  public synchronized List<QueueNode> readAndClearQueue() {
    return new ArrayList<>(this.networkQueue.readAndClear());
  }
  public synchronized String getQueueToString() {
    return this.networkQueue.queueNodesToString();
  }

}
