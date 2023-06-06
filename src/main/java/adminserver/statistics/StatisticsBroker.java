package adminserver.statistics;

/**
 * The MQTT Broker on which Greenfield relies is online at the following
 * address: tcp://localhost:1883.
 * The cleaning robots of Greenfield use this broker to periodically communicate 
 * the air pollution measurements to the Administrator Server. As will
 * be described in the next section, each robot publishes such measurements to
 * the MQTT topic dedicated to the district in which the robot operates. The
 * Administrator Server subscribes to all the topics to receive the air pollution
 * measurements from each district.
 */
public class StatisticsBroker implements Runnable {
  @Override
  public void run() {
    try {
        // brew services start mosquitto

        String brokerCommand = "/usr/local/sbin/mosquitto"; // or the path to your MQTT broker executable
        ProcessBuilder builder = new ProcessBuilder(brokerCommand);
        builder.inheritIO(); // redirect the child process's standard output and error to the parent process's standard output and error
        Process process = builder.start();
        System.out.println("MQTT broker started");

        int exitCode = process.waitFor(); // wait for the child process to exit

        // System.out.println("MQTT broker exited with code " + exitCode);
      } catch (Exception e) {
        e.printStackTrace();
      }
  }
}
