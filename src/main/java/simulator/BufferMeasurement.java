package simulator;

import java.util.ArrayList;
import java.util.List;

import robot.Robot;
import utils.MeasurementRecord;

/**
 * Each simulator is a thread that consists of an infinite loop that 
 * periodically generates (with a pre-defined frequency) the simulated measurements.
 * Such measurements are added to a proper data structure. We only provide
 * the interface (Buffer ) of this data structure that exposes two methods: 
 * void add(Measurement m), List <Measurement> readAllAndClean().
 * Thus, it is necessary to create a class that implements this interface. Note
 * that each cleaning robot is equipped with a single sensor.
 * The simulation thread uses the method addMeasurement to fill the
 * data structure. Instead, the method readAllAndClean, must be used to
 * obtain the measurements stored in the data structure. At the end of a
 * read operation, readAllAndClean makes room for new measurements in
 * the buffer. Specifically, you must process sensor data through the sliding
 * window technique that was introduced in the theory lessons. You must
 * consider a buffer of 8 measurements, with an overlap factor of 50%. When
 * the dimension of the buffer is equal to 8 measurements, you must compute
 * the average of these 8 measurements. A cleaning robot will periodically send
 * these averages to the Administrator Server (as explained in Section 4.2).
 */
public class BufferMeasurement implements Buffer {

  private List<Measurement> measurements;
  private int windowSize = 8;
  private int overlapFactor = 50;
  private List<Double> averages;
  
  private boolean stopPrintMsg = false;

  private static BufferMeasurement instance = null;

  private BufferMeasurement() {
    this.measurements = new ArrayList<Measurement>();
    this.averages = new ArrayList<Double>();
  }

  public static BufferMeasurement getInstance() {
    if (instance == null) {
      instance = new BufferMeasurement();
    }
    return instance;
  }

  @Override
  public synchronized void addMeasurement(Measurement toAdd) {
    this.measurements.add(toAdd);
    if (!this.stopPrintMsg) {
      System.out.println("Buffer: Added measurement: " + toAdd + ", buffer size: " + this.measurements.size());
    }
    //! compute sliding window
    if (this.measurements.size() == this.windowSize) {
      // compute average
      double avg = 0.0;
      for (Measurement m : this.measurements) {
        avg += m.getValue();
      }
      avg /= this.measurements.size();
      this.averages.add(avg);
      // remove first half of the window
      for (int i = 0; i < windowSize*overlapFactor/100; i++) {
        this.measurements.remove(0);
      }
      if (!this.stopPrintMsg) {
        System.out.println("Buffer: reached windowsSize(" + this.windowSize
            + "), computed average: " + avg + " and keeped last " + this.measurements.size() + " measurements");
      }
      this.stopPrintMsg = true;
    }
  }

  @Override
  public synchronized List<Measurement> readAllAndClean() {
    List<Measurement> toReturn = new ArrayList<Measurement>(this.measurements);
    this.measurements.clear();
    this.averages.clear();
    return toReturn;
  }

  public synchronized MeasurementRecord createMeasurementRecord() {
    List<Double> avgs = new ArrayList<Double>(this.averages);
    this.readAllAndClean();
    return new MeasurementRecord(
      Robot.getInstance().getId(),
      System.currentTimeMillis(),
      avgs
    );
  }
  
}
