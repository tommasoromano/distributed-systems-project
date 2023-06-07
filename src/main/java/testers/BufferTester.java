package testers;

import simulator.BufferMeasurement;
import simulator.PM10Simulator;
import simulator.Simulator;
import simulator.MeasurementRecord;

public class BufferTester {
  public static void main(String[] args) {
    System.out.println("BufferTester");

    Thread pm10Thread = new Thread(
        new PM10Simulator(
        "1234",
        BufferMeasurement.getInstance()
      )
    );
    pm10Thread.start();

    for (int i = 0; i < 10; i++) {
      int ts = i;
      Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            // random sleep
            int rand = ((int)Math.floor(Math.random() * (5 - 0 + 1) + 0));
            Thread.sleep(ts * 10000);
            System.out.println(MeasurementRecord.toJson(BufferMeasurement.getInstance().createMeasurementRecord()));
          } catch (Exception e) {
            // e.printStackTrace();
          }
        }
      });
      t.start();
    }

  }
}
