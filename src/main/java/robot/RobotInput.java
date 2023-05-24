package robot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ConnectException;

import utils.City;

public class RobotInput implements Runnable {

  public RobotInput() {
  }

  @Override
  public void run() {
    System.out.println("RobotInput running...");
    
    Robot.getInstance().setCityId(City.selectCityStdInput().getId());

    // robot init
    while(true) {
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      String input;

      System.out.println("Registering a new robot for "+City.getCityById(Robot.getInstance().getCityId()).getName()+":");

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

        Robot.getInstance().init(id, ipAddress, portNumber);

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
      System.out.println("quit. Leave the network and quit");
      System.out.println("fix.  Start maintenance");

      try {
        input = br.readLine();
        switch (input) {
          case "quit":
            Robot.getInstance().disconnect();
            break;
          case "fix":
            Robot.getInstance().startMaintenance();
            break;
          default:
            System.out.println("Invalid option.");
            break;
        }
      } catch (Exception e) {
        System.out.println("Error: "+e.getMessage());
      }
    }

  }
}
