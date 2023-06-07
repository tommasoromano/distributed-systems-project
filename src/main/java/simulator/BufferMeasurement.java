package simulator;

import java.util.ArrayList;
import java.util.List;

import robot.Robot;
import utils.Config;

/**
 * ...
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
  private List<Measurement> averages;
  
  private boolean stopPrintMsg = false;

  private static BufferMeasurement instance = null;

  private BufferMeasurement() {
    this.measurements = new ArrayList<Measurement>();
    this.averages = new ArrayList<Measurement>();
  }

  public static BufferMeasurement getInstance() {
    if (instance == null) {
      instance = new BufferMeasurement();
    }
    return instance;
  }

  @Override
  public synchronized void addMeasurement(Measurement toAdd) {
    testThreadSleep("addMeasurement "+toAdd.toString());

    this.measurements.add(toAdd);
    if (!this.stopPrintMsg) {
      System.out.println("Buffer: Added measurement: " + toAdd + ", buffer size: " + this.measurements.size());
    }
    if (this.measurements.size() == Config.BUFFER_SENSOR_WINDOW_SIZE) {
      // compute average
      double avg = 0.0;
      for (Measurement m : this.measurements) {
        avg += m.getValue();
      }
      avg /= this.measurements.size();
      this.averages.add(new Measurement(
        this.measurements.get(0).getId(), 
        this.measurements.get(0).getType(), 
        avg, 
        System.currentTimeMillis())
      );
      // remove first half of the window
      for (int i = 0; i < Config.BUFFER_SENSOR_WINDOW_SIZE*Config.BUFFER_SENSOR_OVERLAP_FACTOR/100; i++) {
        this.measurements.remove(0);
      }
      if (!this.stopPrintMsg) {
        System.out.println("Buffer: reached windowsSize(" + Config.BUFFER_SENSOR_WINDOW_SIZE
            + "), computed average: " + avg + " and keeped last " + this.measurements.size() + " measurements");
      }
      this.stopPrintMsg = true;
    }
  }

  @Override
  public synchronized List<Measurement> readAllAndClean() {
    testThreadSleep("readAllAndClean");

    List<Measurement> toReturn = new ArrayList<Measurement>(this.averages);
    this.measurements.clear();
    this.averages.clear();
    return toReturn;
  }

  public MeasurementRecord createMeasurementRecord() {
    // testThreadSleep("createMeasurementRecord");

    List<Measurement> MeasurementAvgs = this.readAllAndClean();
    List<Double> avgs = new ArrayList<Double>();
    for (Measurement m : MeasurementAvgs) {
      avgs.add(m.getValue());
    }
    return new MeasurementRecord(
      Robot.getInstance().getId(),
      System.currentTimeMillis(),
      avgs
    );
  }

	private void testThreadSleep(String msg) {
		if (Config.RESOURCE_THREAD_SLEEP_BUFFER <= 0) {
			return;
		}
		System.out.println("[Thread start sleep] Buffer: " + msg);
		try {
			Thread.sleep(Config.RESOURCE_THREAD_SLEEP*1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("[Thread end sleep] Buffer: " + msg);
	}
  
}
