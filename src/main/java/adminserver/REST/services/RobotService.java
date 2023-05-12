package adminserver.REST.services;

import java.util.List;

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

import adminserver.REST.beans.RobotBean;
import adminserver.AdministratorServer;
import utils.City;

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

      System.out.println("Received REST POST /{cityId}/insert"
                        + "\n\tcityId:      "+cityId
                        + "\n\tid:          "+id
                        + "\n\tipAddress:   "+ipAddress
                        + "\n\tportNumber:  "+portNumber);
        
      // check if cityId is in the list of cities
      if (!City.isValidCityId(cityId)) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }

      // create robot
      try {
        RobotBean robotBean = new RobotBean(id, ipAddress, portNumber);
        return Response.ok(AdministratorServer.getInstance(cityId).addRobot(robotBean)).build();

      } catch (Exception e) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }

    }

    @DELETE
    @Path("/{cityId}/remove/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeRobot(@PathParam("cityId") int cityId,
                                @PathParam("id") int id
      ) {

      System.out.println("Received REST DELETE /{cityId}/remove/{id}"
                        + "\n\tcityId:      "+cityId
                        + "\n\tid:          "+id);

      // check if cityId is in the list of cities
      if (!City.isValidCityId(cityId)) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }

      // check if exists a robot with the same id
      for (RobotBean robotBean : AdministratorServer.getInstance(cityId).getRobots()) {
        if (robotBean.getId() == id) {
          AdministratorServer.getInstance(cityId).removeRobotById(id);
          return Response.ok("Robot "+id+" removed with success").build();
        }
      }
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Path("/{cityId}/robots")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRobots(@PathParam("cityId") int cityId) {

      System.out.println("Received REST GET /{cityId}/robots"
                        + "\n\tcityId:      "+cityId);

      // check if cityId is in the list of cities
      if (!City.isValidCityId(cityId)) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }

      return Response.ok(AdministratorServer.getInstance(cityId).getRobots()).build();
    }

    @GET
    @Path("/{cityId}/pollution/avg_id_n/{id}/{n}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNPollutionOfRobotId( @PathParam("cityId") int cityId,
                                            @PathParam("id") int id,
                                            @PathParam("n") int n
      ) {

      System.out.println("Received REST GET /{cityId}/pollution/avg_n/{id}/{n}"
                        + "\n\tcityId:      "+cityId
                        + "\n\tid:          "+id
                        + "\n\tn:           "+n);

      // check if cityId is in the list of cities
      if (!City.isValidCityId(cityId)) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }

      // check if exists a robot with the same id
      for (RobotBean robotBean : AdministratorServer.getInstance(cityId).getRobots()) {
        if (robotBean.getId() == id) {
          return Response.ok(
            AdministratorServer.getInstance(cityId).getStatistics().getAvgLastNByRobotId(id, n)
          ).build();
        }
      }
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Path("/{cityId}/pollution/avg_t1_t2/{t1}/{t2}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAvgPollutionBetweenT1AndT2(@PathParam("cityId") int cityId,
                                                  @PathParam("t1") long t1,
                                                  @PathParam("t2") long t2
      ) {

      System.out.println("Received REST GET /{cityId}/pollution/avg_t1_t2/{t1}/{t2}"
                        + "\n\tcityId:      "+cityId
                        + "\n\tt1:          "+t1
                        + "\n\tt2:          "+t2);

      // check if cityId is in the list of cities
      if (!City.isValidCityId(cityId)) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }

      return Response.ok(
        AdministratorServer.getInstance(cityId).getStatistics().getAvgBetweenTimestamps(t1, t2)
      ).build();
    }

    @GET
    @Path("/{cityId}/city")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCityRepresentation(@PathParam("cityId") int cityId) {

      System.out.println("Received REST GET /{cityId}/city"
                        + "\n\tcityId:      "+cityId);

      // check if cityId is in the list of cities
      if (!City.isValidCityId(cityId)) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }

      return Response.ok(AdministratorServer.getInstance(cityId).getCityRepresentation()).build();
    }

    @GET
    @Path("/{cityId}/pollution/db")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMeasurementDB(@PathParam("cityId") int cityId) {

      System.out.println("Received REST GET /{cityId}/pollution/db"
                        + "\n\tcityId:      "+cityId);

      // check if cityId is in the list of cities
      if (!City.isValidCityId(cityId)) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }

      return Response.ok(AdministratorServer.getInstance(cityId).getStatistics().toDBRepersentation()).build();
    }

}