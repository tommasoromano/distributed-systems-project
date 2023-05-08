package adminserver;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;

public class StartAdminServer {

  public static void main(String[] args) throws Exception {

      System.out.println("Starting server...");

      City city = City.greenfieldCity;
      System.out.println("City: "+city.getName());

      AdministratorServer administratorServer = AdministratorServer.getInstance(city.getId());

      HttpServer server = HttpServerFactory.create("http://"+city.getHost()+":"+city.getPort()+"/");
      server.start();

      System.out.println("Server started on: http://"+city.getHost()+":"+city.getPort());
  }
}
