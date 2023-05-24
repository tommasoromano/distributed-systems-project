package robot.communication;

import robot.Robot;

import java.util.Timer;
import java.util.TimerTask;


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
 */
public class RobotMaintenance implements Runnable {

  private long malfunctionLoop = 10;
  private float changeOfMalfunction = 0.1f;

  public RobotMaintenance() {}

  @Override
  public void run() {

    Timer timer = new Timer();
    TimerTask task = new TimerTask() {
        @Override
        public void run() {
          goToMaintenance();
        }
    };

    // Schedule the task to run every 15 seconds
    timer.schedule(task, malfunctionLoop*1000, malfunctionLoop*1000);

  }

  public void goToMaintenance() {
    if (Math.random() < changeOfMalfunction) {
      System.out.println("Malfunction: "+ Robot.getInstance().getId()+" is going to maintenance");
      // ask for permission to coordinator to go to maintenance
      Robot.getInstance().getNetwork().askForMaintenance();
      try {
        Robot.getInstance().startMaintenance();
        Thread.sleep(10*1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      // release permission to coordinator to go to maintenance
      Robot.getInstance().endMaintenance();
      Robot.getInstance().getNetwork().hasFinishedMaintenance();
    }
  }

}
