package adminserver.statistics;

import java.util.ArrayList;
import java.util.List;

import simulator.Measurement;

// import org.apache.spark.sql.RowFactory;
// import org.apache.spark.sql.Dataset;
// import org.apache.spark.sql.Row;
// import org.apache.spark.sql.SparkSession;
// import org.apache.spark.sql.types.DataTypes;
// import org.apache.spark.sql.types.StructField;
// import org.apache.spark.sql.types.StructType;

public class StatisticsSpark {

  // private SparkSession spark;
  // private Dataset<Row> dataFrame;

  // private StatisticsDB() {

  //   // Create SparkSession
  //   this.spark = SparkSession.builder()
  //       .appName("Statistics")
  //       .master("local[*]")
  //       .getOrCreate();

  //   // Create DataFrame schema
  //   StructType schema = DataTypes.createStructType(new StructField[] {
  //       DataTypes.createStructField("timestamp", DataTypes.LongType, true),
  //       DataTypes.createStructField("districtId", DataTypes.IntegerType, true),
  //       DataTypes.createStructField("robotId", DataTypes.StringType, true),
  //       DataTypes.createStructField("type", DataTypes.StringType, true),
  //       DataTypes.createStructField("value", DataTypes.DoubleType, true)
  //   });

  //   // Create empty DataFrame
  //   this.dataFrame = spark.createDataFrame((List<Row>) spark.emptyDataFrame(), schema);

  //   // Show the empty DataFrame structure
  //   System.out.println("Created a Spark DataFrame to record measurements:");
  //   this.dataFrame.printSchema();
  // }
  // private static StatisticsDB instance = null;
  // public synchronized static StatisticsDB getInstance() {
  //   if (instance == null) {
  //     instance = new StatisticsDB();
  //   }
  //   return instance;
  // }

  // public synchronized void addMeasurement(int districtId, Measurement measurement) {
    
  //   // Create a new DataFrame with the new measurement
  //   List<Row> data = new ArrayList<>();
  //   data.add(RowFactory.create(
  //           measurement.getTimestamp(), 
  //           districtId,
  //           measurement.getId(),
  //           measurement.getType(),
  //           measurement.getValue()
  //   ));
  //   Dataset<Row> newDF = spark.createDataFrame(data, this.dataFrame.schema());

  //   System.out.println("Adding a new measurement to the Spark DataFrame:");
  //   newDF.show();

  //   // Append the new DataFrame to the old one
  //   this.dataFrame = this.dataFrame.union(newDF);
  // }

}
