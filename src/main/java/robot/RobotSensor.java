package robot;

import java.util.Timer;
import java.util.TimerTask;

import simulator.BufferMeasurement;
import simulator.PM10Simulator;
import simulator.Simulator;
import utils.MeasurementRecord;

/**
 * Every 15 seconds, each cleaning robot has to communicate to the Administrator 
 * Server the list of the averages of the air pollution measurements
 * computed after the last communication with the server. This list of averages
 * must be sent to the server associated with: 
 * The ID of the cleaning robot
 * The timestamp in which the list was computed
 * As already anticipated, the communication of the air pollution measurements 
 * must be handled through MQTT. In particular, a cleaning robot
 * that operates in the district i will publish such data on the following MQTT
 * topic: greenfield/pollution/district{i}
 * 
 * Each cleaning robot is equipped with a sensor that periodically detects the
 * air pollution level of Greenfield. Each pollution sensor periodically produces
 * measurements of the level of fine particles in the air (PM10). Every single
 * measurement is characterized by:
 * PM10 value
 * Timestamp of the measurement, expressed in milliseconds
 * The generation of such measurements is produced by a simulator. In order
 * to simplify the project implementation, it is possible to download the code
 * of the simulator directly from the page of the course on Moodle, under
 * the section Projects. Each simulator assigns the number of seconds after
 * midnight as the timestamp associated with a measurement. The code of the
 * simulator must be added as a package to the project, and it must not be
 * modified. During the initialization step, each cleaning robot launches the
 * simulator thread that will generate the measurements for the air pollution
 * sensor.
 */
public class RobotSensor implements Runnable {

  private Thread thisThread;
  private Simulator pm10Simulator;
  private Thread pm10Thread;
  private BufferMeasurement pm10Buffer;
  // private Timer scheduler;

  @Override
  public void run() {
    
    // start PM10 simulator
    this.pm10Buffer = BufferMeasurement.getInstance();
    this.pm10Simulator = new PM10Simulator(
      ""+Robot.getInstance().getId(), 
      this.pm10Buffer
    );
    this.pm10Thread = new Thread(this.pm10Simulator);
    this.pm10Thread.start();

    System.out.println("RobotSensor: started.");

    // this.scheduler = new Timer();
    // TimerTask task = new TimerTask() {
    //     @Override
    //     public void run() {
    //       sendPollutionLevel();
    //     }
    // };
    // this.scheduler.schedule(task, 15*1000, 15*1000);

    while(true){
      try {
        Thread.sleep(15*1000);
        sendPollutionLevel();
      } catch (InterruptedException e) {
        // System.out.println("RobotSensor: interrupted.");
        break;
      }
    }

  }

  public void sendPollutionLevel() {

    MeasurementRecord pm10Measurements = pm10Buffer.createMeasurementRecord();

    // System.out.println("RobotSensor: read " + pm10Measurements.getAverages().size() + " measurements.");
    
    Robot.getInstance().getCommunication().sendPollutionLevel(
      new String(MeasurementRecord.toJson(pm10Measurements))
    );
  }


  public void start() {
    thisThread = new Thread(this);
    thisThread.start();
  }
  public void destroy() {
    try {
      this.pm10Simulator.stopMeGently();
      // this.pm10Thread.interrupt();
    } catch (Exception e) {
      // System.out.println("RobotSensor: error while destroying.");
    }
    // this.scheduler.cancel();
    System.out.println("RobotSensor: destroyed.");
    thisThread.interrupt();
  }

}
