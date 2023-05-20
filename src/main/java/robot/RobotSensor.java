package robot;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import simulator.BufferMeasurement;
import simulator.Measurement;
import simulator.PM10Simulator;
import simulator.Simulator;

public class RobotSensor implements Runnable {

  private Simulator pm10Simulator;
  private Thread pm10Thread;
  private BufferMeasurement pm10Buffer;

  @Override
  public void run() {

    Timer timer = new Timer();
    TimerTask task = new TimerTask() {
        @Override
        public void run() {
          sendAvgPollutionLevel();
        }
    };

    // Schedule the task to run every 15 seconds
    timer.schedule(task, 15000, 15000);

    // start PM10 simulator
    this.pm10Buffer = BufferMeasurement.getInstance();
    this.pm10Simulator = new PM10Simulator(
      ""+Robot.getInstance().getId(), 
      this.pm10Buffer
    );
    this.pm10Thread = new Thread(this.pm10Simulator);
    this.pm10Thread.start();

    System.out.println("RobotSensor: started.");
  }

  public void sendAvgPollutionLevel() {
    long currTimestamp = System.currentTimeMillis();

    List<Measurement> pm10Measurements = this.pm10Buffer.readAllAndClean();
    double pm10Avg = 0.0;
    String pm10Id = "";
    for (Measurement m : pm10Measurements) {
      pm10Avg += m.getValue();
      pm10Id = m.getId();
    }
    pm10Avg /= pm10Measurements.size();

    System.out.println("RobotSensor: read " + pm10Measurements.size() + " measurements, avg: " + pm10Avg);
    
    Robot.getInstance().getCommunication().sendAvgPollutionLevel(
      currTimestamp, pm10Id, pm10Avg
    );
  }

}
