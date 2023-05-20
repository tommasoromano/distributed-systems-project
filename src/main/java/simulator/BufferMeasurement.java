package simulator;

import java.util.ArrayList;
import java.util.List;

public class BufferMeasurement implements Buffer {

  private List<Measurement> measurements;

  private static BufferMeasurement instance = null;

  private BufferMeasurement() {
    this.measurements = new ArrayList<Measurement>();
  }

  public static BufferMeasurement getInstance() {
    if (instance == null) {
      instance = new BufferMeasurement();
    }
    return instance;
  }

  @Override
  public synchronized void addMeasurement(Measurement m) {
    this.measurements.add(m);
  }

  @Override
  public synchronized List<Measurement> readAllAndClean() {
    List<Measurement> temp = new ArrayList<>(this.measurements);
    this.measurements.clear();
    return temp;
  }
  
}
