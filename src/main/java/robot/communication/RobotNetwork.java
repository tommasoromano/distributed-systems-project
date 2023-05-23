package robot.communication;

import java.util.List;

import adminserver.REST.beans.RobotBean;
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

  private boolean inMaintenance = false;

  public RobotNetwork(Position position, List<RobotBean> startRobots) {
    this.position = position;
    this.robots = robots;

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

  private void welcomeAll() {
    // send <entered, robotBean, timestamp> to all robots
  }

  public void goToMaintenance() {
    // send <maintenance, robotBean, timestamp> to coordinator

    // if no response, start election
  }

  public void finishMaintenance() {
    // send <finished, robotBean, timestamp> to all robots
  }

  private void startElection() {
    // send <election, robotBean, timestamp> to all robots with id > this.id

    // if no response, send <coordinator, robotBean, timestamp> to all robots with id < this.id
  }
}
