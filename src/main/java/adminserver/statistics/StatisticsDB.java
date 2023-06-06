package adminserver.statistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utils.Config;
import simulator.MeasurementRecord;

public class StatisticsDB {

  private List<MeasurementRecord> measurements;
  
  private Map<Integer,Integer> sensorCounter = new HashMap<Integer,Integer>();

  private StatisticsDB() {
    this.measurements = new ArrayList<MeasurementRecord>();
  }
  private static StatisticsDB instance = null;
  public static StatisticsDB getInstance() {
    if (instance == null) {
      instance = new StatisticsDB();
    }
    return instance;
  }

  public void addMeasurement(MeasurementRecord measurement) {
    // this.measurements.add(new MeasurementRecord(districtId, measurement.getRobotId(), measurement));
    this.measurements.add(measurement);

    // print every n-th sensor measurement
    if (!this.sensorCounter.containsKey(measurement.getRobotId()) || this.sensorCounter.get(measurement.getRobotId()) % Config.PRINT_SENSOR_EVERY == 0) {
      this.sensorCounter.put(measurement.getRobotId(), 0);
      System.out.println("StatisticsDB: added measurement: " + measurement.toString());
    }
    this.sensorCounter.put(measurement.getRobotId(), this.sensorCounter.get(measurement.getRobotId()) + 1);
  }
  public List<MeasurementRecord> getAllMeasurements() {
    return this.measurements;
  }

  public double getAvgLastNByRobotId(int robotId, int n) {
    List<MeasurementRecord> filtered = this.filterByRobotId(robotId);
    if (filtered.size() < n) {
      return -1;
    }
    List<MeasurementRecord> lastN = filtered.subList(filtered.size() - n, filtered.size());
    return this.calculateAvgOfMeasurements(lastN);
  }
  public double getAvgBetweenTimestamps(long t1, long t2) {
    List<MeasurementRecord> betweenTimestamps = this.filterBetweenTimestamps(t1, t2);
    if (betweenTimestamps.size() == 0) {
      return -1;
    }
    return this.calculateAvgOfMeasurements(betweenTimestamps);
  }

  public void removeRecordsNotInValidRobotIds(List<Integer> validRobotIds) {
    List<MeasurementRecord> filtered = new ArrayList<MeasurementRecord>();
    for (MeasurementRecord record : this.measurements) {
      if (validRobotIds.contains(record.getRobotId())) {
        filtered.add(record);
      }
    }
    this.measurements = filtered;
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
      double avg = 0;
      for (Double val : measurement.getAverages()) {
        avg += val;
      }
      avg /= measurement.getAverages().size();
      sum += avg;
    }
    return sum / measurements.size();
  }

  public String dbToString() {
    String str = "StatisticsDB:\n";
    for (MeasurementRecord record : this.measurements) {
      str += "\t" + record.toString() + "\n";
    }
    return str;
  }

}
