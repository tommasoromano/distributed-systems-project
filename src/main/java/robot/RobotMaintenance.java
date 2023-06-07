package robot;

import robot.IRobotComponent;
import robot.Robot;
import utils.Config;

import java.util.Timer;
import java.util.TimerTask;


/**
 * Every 10 seconds, each cleaning robot has a chance of 10% to be subject
 * to malfunctions. In this case, the cleaning robot must go to the mechanic
 * of Greenfield (for simplicity, it is not necessary to simulate that the robot
 * actually reaches the mechanic in the smart city grid). The mechanic may
 * be accessed only by a single cleaning robot at a time. It is also possible
 * to explicitly ask a cleaning robot to go to the mechanic through a specific
 * command (i.e., fix ) on the command line. [...]
 * The maintenance operation is simulated through a Thread.sleep() of
 * 10 seconds
 * 
 * 
 * [...] When a robot wants to leave the system in a controlled way, it must
 * follow the next steps:
 * • complete any operation at the mechanic [...]
 */
public class RobotMaintenance implements Runnable, IRobotComponent {

  private Thread thisThread;

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
    while (true) {
      if (Math.random() < Config.MALFUNCTION_CHANCE) {
        Robot.getInstance().getMaintenance().goToMaintenance();
      }
      try {
        Thread.sleep(Config.MALFUNCTION_CHANCE_LOOP*1000);
      } catch (InterruptedException e) {
        // e.printStackTrace();
        break;
      }
    }
  }

  public synchronized void goToMaintenance() {
    if (this.state != State.OUT) {
      return;
    }
    System.out.println("Maintenance: "+ Robot.getInstance().getId()+" needs to go to maintenance");
    this.askedTimestamp = System.currentTimeMillis();
    this.state = State.ASK;
    Robot.getInstance().getNetwork().askForMaintenance();
  }

  public synchronized void maintenanceGranted() {
    if (this.state == State.IN) {
      return;
    }
    System.out.println("Maintenance: granted");
    this.state = State.IN;
    Thread maintenanceThread = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Thread.sleep(Config.MALFUNCTION_LENGTH*1000);
        } catch (InterruptedException e) {
          // e.printStackTrace();
        }
        System.out.println("Maintenance: finished");
        setState(State.OUT);
        Robot.getInstance().getNetwork().hasFinishedMaintenance();
        if (disconnectAtMaintenanceEnd) {
          Robot.getInstance().disconnect();
        }
      }
    });
    maintenanceThread.start();
  }

  private boolean disconnectAtMaintenanceEnd = false;
  public void setDisconnectAtMaintenanceEnd() {
    disconnectAtMaintenanceEnd = true;
  }

  private synchronized void setState(State state) {
    this.state = state;
  }

  public synchronized State getState() {
    return this.state;
  }

  public synchronized long getAskedTimestamp() {
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
