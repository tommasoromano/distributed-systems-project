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

  private Thread thisThread;

  private long malfunctionLoop = 10;
  private float changeOfMalfunction = 0.01f;
  private int malfunctionLength = 10;

  public enum State {
    ASK,
    IN,
    OUT
  }
  private State state;
  private long askedTimestamp;
  // private Timer scheduler;

  public RobotMaintenance() {
    this.state = State.OUT;
  }

  @Override
  public void run() {
    // scheduler = new Timer();
    // TimerTask task = new TimerTask() {
    //     @Override
    //     public void run() {
    //       if (Math.random() < changeOfMalfunction) {
    //         Robot.getInstance().getMaintenance().goToMaintenance();
    //       }
    //     }
    // };
    // scheduler.schedule(task, malfunctionLoop*1000, malfunctionLoop*1000);

    while (true) {
      if (Math.random() < changeOfMalfunction) {
        Robot.getInstance().getMaintenance().goToMaintenance();
      }
      try {
        Thread.sleep(malfunctionLoop*1000);
      } catch (InterruptedException e) {
        // e.printStackTrace();
        break;
      }
    }

  }

  public void goToMaintenance() {
    if (getState() != State.OUT) {
      return;
    }
    System.out.println("Maintenance: "+ Robot.getInstance().getId()+" needs to go to maintenance");
    setState(State.ASK);
    Robot.getInstance().getNetwork().askForMaintenance();
  }

  public void maintenanceGranted() {
    if (getState() == State.IN) {
      return;
    }
    System.out.println("Maintenance: granted");
    setState(State.IN);
    try {
      Thread.sleep(malfunctionLength*1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    System.out.println("Maintenance: finished");
    setState(State.OUT);
    Robot.getInstance().getNetwork().hasFinishedMaintenance();
  }

  private synchronized void setState(State state) {
    this.state = state;
  }

  public synchronized State getState() {
    return this.state;
  }

  public long getAskedTimestamp() {
    return this.askedTimestamp;
  }


  public void start() {
    thisThread = new Thread(this);
    thisThread.start();
  }
  public void destroy() {
    // scheduler.cancel();
    System.out.println("Maintenance: destroyed");
    thisThread.interrupt();
  }

}
