package adminserver.REST.services;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import adminserver.REST.beans.Robot;
import adminserver.AdministratorServer;
import adminserver.City;

@Path("/robots")
public class RobotService {

    @POST
    @Path("/{cityId}/insert")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response insertRobot(@PathParam("cityId") int cityId,
                                @FormParam("id") int id,
                                @FormParam("ipAddress") String ipAddress,
                                @FormParam("portNumber") int portNumber
      ) {

      System.out.println("REST Inserting robot "+id+", "+ipAddress+":"+portNumber);
        
      // check if cityId is in the list of cities
      if (!City.isValidCityId(cityId)) {
        System.out.println(">>> INSERT: City "+cityId+" not found");
        return Response.status(Response.Status.NOT_FOUND).build();
      }

      // check if already exists a robot with the same id
      for (Robot robot : AdministratorServer.getInstance(cityId).getRobots()) {
        if (robot.getId() == id) {
          System.out.println(">>> INSERT: Robot "+id+" already exists");
          return Response.status(Response.Status.CONFLICT).build();
        }
      }

      // check if id, ipAddress and portNumber exist and are valid
      if (id <= 0 || ipAddress.equals("") || portNumber <= 0) {
        System.out.println(">>> INSERT: Invalid robot data");
        return Response.status(Response.Status.BAD_REQUEST).build();
      }

      // create robot
      Robot robot = new Robot(id, ipAddress, portNumber);
      AdministratorServer.getInstance(cityId).addRobot(robot);

      System.out.println(">>> INSERT: Robot "+id+", "+ipAddress+":"+portNumber+" inserted with success");
      return Response.ok("Robot "+id+", "+ipAddress+":"+portNumber+" inserted with success").build();
    }

    @DELETE
    @Path("/{cityId}/remove/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeRobot(@PathParam("cityId") int cityId,
                                @PathParam("id") int id
      ) {

      System.out.println("REST Removing robot "+id);

      // check if cityId is in the list of cities
      if (!City.isValidCityId(cityId)) {
        System.out.println(">>> REMOVE: City "+cityId+" not found");
        return Response.status(Response.Status.NOT_FOUND).build();
      }

      // check if exists a robot with the same id
      for (Robot robot : AdministratorServer.getInstance(cityId).getRobots()) {
        if (robot.getId() == id) {
          AdministratorServer.getInstance(cityId).removeRobotById(id);
          System.out.println(">>> REMOVE: Robot "+id+" removed with success");
          return Response.ok("Robot "+id+" removed with success").build();
        }
      }
      System.out.println(">>> REMOVE: Robot "+id+" not found");
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Path("/{cityId}/robots")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRobots(@PathParam("cityId") int cityId) {

      System.out.println("REST Getting robots");

      // check if cityId is in the list of cities
      if (!City.isValidCityId(cityId)) {
        System.out.println(">>> GET: City "+cityId+" not found");
        return Response.status(Response.Status.NOT_FOUND).build();
      }

      System.out.println(">>> GET: City "+cityId+" robots returned with success");
      return Response.ok(AdministratorServer.getInstance(cityId).getRobots()).build();
    }

    @GET
    @Path("/{cityId}/pollution/avg_n/{id}/{n}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNPollutionOfRobotId( @PathParam("cityId") int cityId,
                                            @PathParam("id") int id,
                                            @PathParam("n") int n
      ) {

      // check if cityId is in the list of cities
      if (!City.isValidCityId(cityId)) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }

      // check if exists a robot with the same id
      for (Robot robot : AdministratorServer.getInstance(cityId).getRobots()) {
        if (robot.getId() == id) {
          return Response.ok().build();
        }
      }
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Path("/{cityId}/pollution/avg_t1_t2/{t1}/{t2}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAvgPollutionBetweenT1AndT2(@PathParam("cityId") int cityId,
                                                  @PathParam("t1") int t1,
                                                  @PathParam("t2") int t2
      ) {

      // check if cityId is in the list of cities
      if (!City.isValidCityId(cityId)) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }

      return Response.ok().build();
    }

}