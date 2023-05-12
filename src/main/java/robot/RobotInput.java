package robot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ConnectException;

import utils.City;

public class RobotInput implements Runnable {

  private Robot robot;
  public RobotInput(Robot robot) {
    this.robot = robot;
  }

  @Override
  public void run() {
    System.out.println("RobotInput running...");
    
    robot.setCity(City.selectCityStdInput());

    // robot init
    while(true) {
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      String input;

      System.out.println("Registering a new robot for "+robot.getCity().getName()+":");

      try {
        System.out.println("Enter robot id or \"return\" for a random one:");
        input = br.readLine();
        if (input.equals("")) {
          input = ((int)Math.floor(Math.random() * (9999 - 0 + 1) + 0)) + "";
        }
        int id = Integer.parseInt(input);
        System.out.println("Enter robot ip addres or \"return\" for default:");
        input = br.readLine();
        if (input.equals("")) {
          input = "localhost";
        }
        String ipAddress = input;
        System.out.println("Enter robot port number or \"return\" for a random one:");
        input = br.readLine();
        if (input.equals("")) {
          input = ((int)Math.floor(Math.random() * (9999 - 1000 + 1) + 1000)) + "";
        }
        int portNumber = Integer.parseInt(input);

        this.robot.init(id, ipAddress, portNumber);

        break;
      } catch (Exception e) {
        System.out.println(e.getMessage());
      }
    }

    // robot menu
    while(true) {
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      String input;

      System.out.println("Robot main menu:");

      try {
        input = br.readLine();
      } catch (Exception e) {
        System.out.println("Error: "+e.getMessage());
      }
    }

  }
}
