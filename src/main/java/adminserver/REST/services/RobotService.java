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

      System.out.println("REST received POST /{cityId}/insert"
                        + "\n\tcityId: "+cityId
                        + "\tid: "+id
                        + "\tipAddress: "+ipAddress
                        + "\tportNumber: "+portNumber);
        
      // check if cityId is in the list of cities
      if (!City.isValidCityId(cityId)) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }

      // create robot
      try {
        RobotBean robotBean = new RobotBean(id, ipAddress, portNumber);
        return Response.ok(AdministratorServer.getInstance(cityId).addRobot(robotBean)).build();

      } catch (Exception e) {
        // System.out.println("Error REST insert: "+e.getMessage());
        return Response.status(Response.Status.BAD_REQUEST).build();
      }

    }

    @DELETE
    @Path("/{cityId}/remove/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeRobot(@PathParam("cityId") int cityId,
                                @PathParam("id") int id
      ) {

      System.out.println("REST received DELETE /{cityId}/remove/{id}"
                        + "\n\tcityId: "+cityId
                        + "\tid: "+id);

      // check if cityId is in the list of cities
      if (!City.isValidCityId(cityId)) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }

      // check if exists a robot with the same id
      // for (RobotBean robotBean : AdministratorServer.getInstance(cityId).getRobots()) {
      //   if (robotBean.getId() == id) {
      //     AdministratorServer.getInstance(cityId).removeRobotById(id);
      //     return Response.ok("Robot "+id+" removed with success").build();
      //   }
      // }

      try {
        AdministratorServer.getInstance(cityId).removeRobotById(id);
        return Response.ok("Robot "+id+" removed with success").build();
      } catch (Exception e) {
        // System.out.println("Error REST remove: "+e.getMessage());
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      // return Response.status(Response.Status.NOT_FOUND).build();
    }

}