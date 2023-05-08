package adminclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import com.sun.jersey.api.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import adminserver.City;
import adminserver.REST.beans.Robot;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.representation.Form;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class AdminClient {
  // https://eclipse-ee4j.github.io/jersey.github.io/documentation/1.19.1/client-api.html
  public static void main(String[] args) throws IOException {
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String input;

    while(true) {
      System.out.println("Select a city to operate: ");
      System.out.println("1. Greenfield");
      input = br.readLine();
      int cityId = Integer.parseInt(input);
      if (City.isValidCityId(cityId)) {
        City city = City.getCityById(cityId);
        System.out.println("Selected city: "+City.getCityById(cityId).getName());

        boolean exit = false;
        while (!exit) {
          System.out.println("Welcome to "+city.getName()+" admin client!");
          System.out.println("Select a REST action: ");
          System.out.println("1. Add robot");
          System.out.println("2. Remove robot");
          System.out.println("3. Get all robots");
          System.out.println("4. Exit from "+city.getName()+" admin client");
          input = br.readLine();
          int option = Integer.parseInt(input);
          switch(option) {
            case 1:
              System.out.println("Add robot:");
              try {
                addRobot(city);
              } catch (Exception e) {
                System.out.println("Error: "+e.getMessage());
              }
              break;
            case 2:
              System.out.println("Remove robot:");
              try {
                removeRobot(city);
              } catch (Exception e) {
                System.out.println("Error: "+e.getMessage());
              }
              break;
            case 3:
              System.out.println("Get all robots:");
              try {
                getAllRobots(city);
              } catch (Exception e) {
                System.out.println("Error: "+e.getMessage());
              }
              break;
            case 4:
              System.out.println("Exited from "+city.getName()+" admin client");
              exit = true;
              break;
            default:
              System.out.println("Invalid option");
              break;
          }

        }

      } else {
        System.out.println("Invalid city id");
      }
    }
  }

  private static void addRobot(City city) {

    Client client = Client.create();
    WebResource resource = client.resource(getBaseURI(city)+"insert");

    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String input;
    int id = -1;
    String ipAddress = "";
    int portNumber = -1;

    // create form parameters
    
    try {
      System.out.println("Enter robot id or \"return\" for random: ");
      input = br.readLine();
      if (input.equals("")) {
        input = ((int)Math.floor(Math.random() * (9999 - 0 + 1) + 0)) + "";
      }
      id = Integer.parseInt(input);
    } catch (IOException e) {
      System.out.println("Error: " + e.getMessage());
      return;
    }

    try {
      System.out.println("Enter robot ip address or \"return\" for default: ");
      input = br.readLine();
      if (input.equals("")) {
        input = "localhost";
      }
      ipAddress = input;
    } catch (IOException e) {
      System.out.println("Error: " + e.getMessage());
      return;
    }

    try {
      System.out.println("Enter robot port number or \"return\" for random: ");
      input = br.readLine();
      if (input.equals("")) {
        input = ((int)Math.floor(Math.random() * (9999 - 1000 + 1) + 1000)) + "";
      }
      portNumber = Integer.parseInt(input);
    } catch (IOException e) {
      System.out.println("Error: " + e.getMessage());
      return;
    }

    System.out.println("You are trying to insert a Robot "+id+", "+ipAddress+":"+portNumber);

    // Create the client

    Form form = new Form();
    form.add("id", id+"");
    form.add("ipAddress", ipAddress);
    form.add("portNumber", portNumber+"");

    String response =  resource.post(String.class, form);

    // // Process the response
    // if (response.getStatus() == Response.Status.OK.getStatusCode()) {
    //     // String responseBody = response.readEntity(String.class);
    //     String responseBody = response.getEntity().toString();
    //     System.out.println("Response: \n" + responseBody);
    // } else {
    //     System.out.println("Error: " + response.getStatus());
    // }

    System.out.println("Response: \n"+response);

  }

  private static void removeRobot(City city) {

    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    String input;
    int id = -1;

    try {
      System.out.println("Enter robot id: ");
      input = br.readLine();
      id = Integer.parseInt(input);
    } catch (IOException e) {
      System.out.println("Error: " + e.getMessage());
      return;
    }

    System.out.println("You are trying to remove Robot "+id);

    Client client = Client.create();
    WebResource resource = client.resource(getBaseURI(city)+"remove/"+id);

    String response = resource.delete(String.class);
    System.out.println("Response: \n" + response);

  }

  private static void getAllRobots(City city) {

    System.out.println("You are trying to get all Robots...");

    Client client = Client.create();
    WebResource resource = client.resource(getBaseURI(city)+"robots");

    String response = resource.get(String.class);
    System.out.println("Response: \n" + response);

  }

  /**
   * http://city.getHost():city.getPort()/robots/city.getId()/
   * @param city
   * @return
   */
  private static String getBaseURI(City city) {
    return "http://"+city.getHost()+":"+city.getPort()+"/robots/"+city.getId()+"/";
  }

}
