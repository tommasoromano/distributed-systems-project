package adminserver.statistics;

public class MeasurementRecord {
  private int districtId;
  private int robotId;
  private long timestamp;
  private String type;
  private double value;
  public MeasurementRecord(int districtId, int robotId, long timestamp, String type, double value) {
    this.districtId = districtId;
    this.robotId = robotId;
    this.timestamp = timestamp;
    this.type = type;
    this.value = value;
  }
  public String toLine() {
    return districtId + "\t" + robotId + "\t" + timestamp + "\t" + type + "\t" + value;
  }
  public static MeasurementRecord parseMeasurementRecord(String line) {
    String[] tokens = line.split("\t");
    if (tokens.length != 5) {
      throw new IllegalArgumentException("Invalid line: " + line);
    }
    int districtId = Integer.parseInt(tokens[0]);
    int robotId = Integer.parseInt(tokens[1]);
    long timestamp = Long.parseLong(tokens[2]);
    String type = tokens[3];
    double value = Double.parseDouble(tokens[4]);
    return new MeasurementRecord(districtId, robotId, timestamp, type, value);
  }

  public int getDistrictId() {
    return districtId;
  }
  public int getRobotId() {
    return robotId;
  }
  public long getTimestamp() {
    return timestamp;
  }
  public String getType() {
    return type;
  }
  public double getValue() {
    return value;
  }
  @Override
  public String toString() {
    return "StatisticsRecord:"
      + "\tdistrictId=" + districtId 
      + "\trobotId=" + robotId
      + "\ttimestamp=" + timestamp
      + "\ttype=" + type
      + "\tvalue=" + value;
  }
}
