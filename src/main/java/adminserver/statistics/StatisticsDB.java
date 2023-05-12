package adminserver.statistics;

import java.util.ArrayList;
import java.util.List;

import simulator.Measurement;

public class StatisticsDB {

  private List<MeasurementRecord> measurements;

  private StatisticsDB() {
    this.measurements = new ArrayList<MeasurementRecord>();
  }
  private static StatisticsDB instance = null;
  public synchronized static StatisticsDB getInstance() {
    if (instance == null) {
      instance = new StatisticsDB();
    }
    return instance;
  }

  public synchronized void addMeasurement(MeasurementRecord measurement) {
    // this.measurements.add(new MeasurementRecord(districtId, measurement.getRobotId(), measurement));
    this.measurements.add(measurement);

    System.out.println("StatisticsDB: added measurement: " + measurement.toString());
  }
  public synchronized List<MeasurementRecord> getAllMeasurements() {
    // List<Measurement> allMeasurements = new ArrayList<Measurement>();
    // for (MeasurementRecord record : this.measurements) {
    //   allMeasurements.add(record.getMeasurement());
    // }
    // return allMeasurements;
    return this.measurements;
  }

  public synchronized double getAvgLastNByRobotId(int robotId, int n) {
    List<MeasurementRecord> filtered = this.filterByRobotId(robotId);
    if (filtered.size() < n) {
      return -1;
    }
    List<MeasurementRecord> lastN = filtered.subList(filtered.size() - n, filtered.size());
    return this.calculateAvgOfMeasurements(lastN);
  }
  public synchronized double getAvgBetweenTimestamps(long t1, long t2) {
    List<MeasurementRecord> betweenTimestamps = this.filterBetweenTimestamps(t1, t2);
    if (betweenTimestamps.size() == 0) {
      return -1;
    }
    return this.calculateAvgOfMeasurements(betweenTimestamps);
  }

  private List<MeasurementRecord> filterByRobotId(int robotId) {
    List<MeasurementRecord> filtered = new ArrayList<MeasurementRecord>();
    for (MeasurementRecord record : this.measurements) {
      if (record.getRobotId() == robotId) {
        filtered.add(record);
      }
    }
    return filtered;
  }
  private List<MeasurementRecord> filterBetweenTimestamps(long t1, long t2) {
    List<MeasurementRecord> filtered = new ArrayList<MeasurementRecord>();
    for (MeasurementRecord record : this.measurements) {
      if (record.getTimestamp() >= t1 && record.getTimestamp() <= t2) {
        filtered.add(record);
      }
    }
    return filtered;
  }

  private double calculateAvgOfMeasurements(List<MeasurementRecord> measurements) {
    double sum = 0;
    for (MeasurementRecord measurement : measurements) {
      sum += measurement.getValue();
    }
    return sum / measurements.size();
  }

  public synchronized String dbToString() {
    String str = "StatisticsDB:\n";
    for (MeasurementRecord record : this.measurements) {
      str += "\t" + record.toString() + "\n";
    }
    return str;
  }

}
