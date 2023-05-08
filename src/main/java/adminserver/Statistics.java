package adminserver;

import java.util.ArrayList;
import java.util.List;

import simulator.Measurement;

public class Statistics {
  private List<Measurement> measurements;
  public Statistics() {
    this.measurements = new ArrayList<Measurement>();
  }
  public synchronized void addMeasurement(Measurement measurement) {
    this.measurements.add(measurement);
  }
  public synchronized List<Measurement> getMeasurements() {
    return this.measurements;
  }
}
