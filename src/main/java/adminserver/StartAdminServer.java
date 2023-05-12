package adminserver;

import utils.City;

public class StartAdminServer {

  public static void main(String[] args) throws Exception {

      System.out.println("Starting Administrator Server...");

      City city = City.selectCityStdInput();

      AdministratorServer administratorServer = AdministratorServer.getInstance(city.getId());

  }
}
