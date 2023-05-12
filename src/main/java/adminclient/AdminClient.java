package adminclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.sun.jersey.api.client.ClientResponse;

import adminserver.REST.RESTutils;
import utils.City;

/**
 * The Administrator Client consists of a simple command-line interface that
 * enables interacting with the REST interface provided by the Administrator
 * Server. Hence, this application prints a straightforward menu to select one of
 * the services offered by the administrator server described in Section 5.3 (e.g.,
 * the list of the smart city robots), and to enter possible required parameters.
 */
public class AdminClient {
  // https://eclipse-ee4j.github.io/jersey.github.io/documentation/1.19.1/client-api.html
  public static void main(String[] args) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String input;

    City city = City.selectCityStdInput();
    int cityId = city.getId();

    boolean exit = false;
    while (!exit) {
      System.out.println("Welcome to "+city.getName()+" admin client!");
      System.out.println("Select a REST action: ");
      System.out.println("\t1. Get all robots");
      System.out.println("\t2. Get average of n measurements of robot");
      System.out.println("\t3. Get average measurement between t1 and t2");
      System.out.println("\t4. Get city representation");
      System.out.println("\t5. Get measurement DB");
      System.out.println("\tany. Exit from "+city.getName()+" admin client");
      input = br.readLine();
      int option = Integer.parseInt(input);
      switch(option) {
        case 1:
          System.out.println("Get all robots:");
          try {
            getAllRobots(cityId);
          } catch (Exception e) {
            System.out.println("Error: "+e.getMessage());
          }
          break;
        case 2:
          System.out.println("Get average of n measurements of robot:");
          try {
            getAvgLastNByRobotId(cityId);
          } catch (Exception e) {
            System.out.println("Error: "+e.getMessage());
          }
          break;
        case 3:
          System.out.println("Get average measurement between t1 and t2:");
          try {
            getAvgBetweenTimestamps(cityId);
          } catch (Exception e) {
            System.out.println("Error: "+e.getMessage());
          }
          break;
        case 4:
          System.out.println("Get city representation:");
          try {
            getCityRepresentation(cityId);
          } catch (Exception e) {
            System.out.println("Error: "+e.getMessage());
          }
          break;
        case 5:
          System.out.println("Get measurement DB");
          try {
            getMeasurementDB(cityId);
          } catch (Exception e) {
            System.out.println("Error: "+e.getMessage());
          }
          break;
        default:
          System.out.println("Exited from "+city.getName()+" admin client");
          exit = true;
          break;
      }

    }

  }

  private static void getAllRobots(int cityId) {

    ClientResponse response = RESTutils.RESTGetAllRobots(cityId);
    
    if (response.getStatus() == 200) {
      System.out.println("All robots:");
      System.out.println(response.getEntity(String.class));
      return;
    }

  }

  private static void getCityRepresentation(int cityId) {

    ClientResponse response = RESTutils.RESTGet(RESTutils.getBaseURI(cityId)+"city");
    
    if (response.getStatus() == 200) {
      System.out.println("City representation:");
      System.out.println(response.getEntity(String.class));
      return;
    }

  }

  private static void getAvgLastNByRobotId(int cityId) {
    
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String input;

    while(true) {
      try {

        System.out.println("Insert robot id:");
        input = br.readLine();
        int robotId = Integer.parseInt(input);

        System.out.println("Insert n:");
        input = br.readLine();
        int n = Integer.parseInt(input);

        ClientResponse response = RESTutils.RESTGetAvgLastNByRobotId(cityId, robotId, n);

        if (response.getStatus() == 200) {
          System.out.println("Average of last "+n+" measurements of robot "+robotId+":");
          System.out.println(response.getEntity(String.class));
          return;
        }

      } catch (Exception e) {
        System.out.println("Error: "+e.getMessage());
      }
    }

  }

  private static void getAvgBetweenTimestamps(int cityId) {

    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String input;

    while(true) {
      try {

        System.out.println("Insert timestamp 1:");
        input = br.readLine();
        long t1 = Long.parseLong(input);

        System.out.println("Insert timestamp 2:");
        input = br.readLine();
        long t2 = Long.parseLong(input);

        ClientResponse response = RESTutils.RESTGetAvgBetweenTimestamps(cityId, t1, t2);

        if (response.getStatus() == 200) {
          System.out.println("Average measurement between "+t1+" and "+t2+":");
          System.out.println(response.getEntity(String.class));
          return;
        }

      } catch (Exception e) {
        System.out.println("Error: "+e.getMessage());
      }
    }

  }

  private static void getMeasurementDB(int cityId) {

    ClientResponse response = RESTutils.RESTGet(RESTutils.getBaseURI(cityId)+"pollution/db");
    
    if (response.getStatus() == 200) {
      System.out.println("Measurement DB:");
      System.out.println(response.getEntity(String.class));
      return;
    }

  }

}
