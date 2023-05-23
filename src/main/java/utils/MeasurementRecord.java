package utils;

import java.util.List;

import com.google.gson.Gson;

public class MeasurementRecord {
  private int robotId;
  private long timestamp;
  private List<Double> averages;

  public MeasurementRecord(int robotId, long timestamp, List<Double> averages) {
    this.robotId = robotId;
    this.timestamp = timestamp;
    this.averages = averages;
  }
  public static String toJson(MeasurementRecord obj) {
    Gson gson = new Gson();
    return gson.toJson(obj);
  }
  public static MeasurementRecord fromJson(String json) {
    Gson gson = new Gson();
    return gson.fromJson(json, MeasurementRecord.class);
}

  public int getRobotId() {
    return robotId;
  }
  public long getTimestamp() {
    return timestamp;
  }
  public List<Double> getAverages() {
    return averages;
  }
  @Override
  public String toString() {
    return "StatisticsRecord:"
      + "\trobotId=" + robotId
      + "\ttimestamp=" + timestamp
      + "\taverages=" + averages;
  }
}
