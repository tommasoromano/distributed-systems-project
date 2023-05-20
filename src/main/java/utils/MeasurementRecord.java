package utils;

public class MeasurementRecord {
  private int robotId;
  private long timestamp;
  private String sensorId;
  private double value;
  public MeasurementRecord(int robotId, long timestamp, String sensorId, double value) {
    this.robotId = robotId;
    this.timestamp = timestamp;
    this.sensorId = sensorId;
    this.value = value;
  }
  public String toJson() {
    return robotId + "\t" + timestamp + "\t" + sensorId + "\t" + value;
  }
  public static MeasurementRecord fromJson(String line) {
    String[] tokens = line.split("\t");
    if (tokens.length != 4) {
      throw new IllegalArgumentException("Invalid line: " + line);
    }
    int robotId = Integer.parseInt(tokens[0]);
    long timestamp = Long.parseLong(tokens[1]);
    String sensorId = tokens[2];
    double value = Double.parseDouble(tokens[3]);
    return new MeasurementRecord(robotId, timestamp, sensorId, value);
  }
  public int getRobotId() {
    return robotId;
  }
  public long getTimestamp() {
    return timestamp;
  }
  public String getSensorId() {
    return sensorId;
  }
  public double getValue() {
    return value;
  }
  @Override
  public String toString() {
    return "StatisticsRecord:"
      + "\trobotId=" + robotId
      + "\ttimestamp=" + timestamp
      + "\tsensorId=" + sensorId
      + "\tvalue=" + value;
  }
}
