package mqttbroker;

public class StatisticsBroker implements Runnable {
  @Override
  public void run() {
    try {
        // brew services start mosquitto

        String brokerCommand = "/usr/local/sbin/mosquitto"; // or the path to your MQTT broker executable
        ProcessBuilder builder = new ProcessBuilder(brokerCommand);
        builder.inheritIO(); // redirect the child process's standard output and error to the parent process's standard output and error
        Process process = builder.start();

        int exitCode = process.waitFor(); // wait for the child process to exit

        System.out.println("MQTT broker exited with code " + exitCode);
      } catch (Exception e) {
        e.printStackTrace();
      }
  }
}
