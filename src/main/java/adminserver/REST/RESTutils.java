package adminserver.REST;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.representation.Form;

import utils.City;

import javax.ws.rs.core.MediaType;

public class RESTutils {

  ////////////////////////////////////////////////////////////
  // COMMON
  ////////////////////////////////////////////////////////////

  private static WebResource createResource(String url) {
    ClientConfig config = new DefaultClientConfig();
    config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    Client client = Client.create(config);
    return client.resource(url);
  }

  public static ClientResponse RESTPost(String url, Form form) {
    
    WebResource resource = createResource(url);

    System.out.println("REST making POST Request: "
      +"\n\tResource: " + resource.getURI()
      +"\tForm: " + form);
    
    ClientResponse response = resource
    .type(MediaType.APPLICATION_FORM_URLENCODED)
    .accept(MediaType.APPLICATION_JSON)
    .post(ClientResponse.class, form);

    System.out.println("REST receiving response POST: "
      +"\n\tResource: " + resource.getURI()
      +"\tForm: " + form
      +"\tResponse: " + response);

    return response;
  }

  public static ClientResponse RESTGet(String url) {
    
    WebResource resource = createResource(url);

    System.out.println("REST making GET Request: "
      +"\n\tResource: " + resource.getURI());
    
    ClientResponse response = resource
    .type(MediaType.APPLICATION_FORM_URLENCODED)
    .accept(MediaType.APPLICATION_JSON)
    .get(ClientResponse.class);

    System.out.println("REST receiving response GET: "
      +"\n\tResource: " + resource.getURI()
      +"\tResponse: " + response);

    return response;
  }

  public static ClientResponse RESTDelete(String url) {
    
    WebResource resource = createResource(url);

    System.out.println("REST making DELETE Request: "
      +"\n\tResource: " + resource.getURI());
    
    ClientResponse response = resource
    .type(MediaType.APPLICATION_FORM_URLENCODED)
    .accept(MediaType.APPLICATION_JSON)
    .delete(ClientResponse.class);

    System.out.println("REST receiving response DELETE: "
      +"\n\tResource: " + resource.getURI()
      +"\tResponse: " + response);

    return response;
  }

  ////////////////////////////////////////////////////////////
  // SPECIFIC
  ////////////////////////////////////////////////////////////

  /**
   * http://city.getHost():city.getPort()/robots/city.getId()/
   * @param city
   * @return
   */
  public static String getRobotsURI(int cityId) {
    City city = City.getCityById(cityId);
    return "http://"+city.getHost()+":"+city.getPort()+"/robots/"+city.getId()+"/";
  }  

  /**
   * http://city.getHost():city.getPort()/clients/city.getId()/
   * @param city
   * @return
   */
  public static String getClientsURI(int cityId) {
    City city = City.getCityById(cityId);
    return "http://"+city.getHost()+":"+city.getPort()+"/clients/"+city.getId()+"/";
  }

  public static ClientResponse RESTPostRobot(int cityId, int id, String ipAddress, int portNumber) {
    Form form = new Form();
    form.add("id", id + "");
    form.add("ipAddress", "localhost");
    form.add("portNumber", ((int)Math.floor(Math.random() * (9999 - 1000 + 1) + 1000)) + "");

    return RESTPost(getRobotsURI(cityId)+"insert", form);
  }

  public static ClientResponse RESTGetAllRobots(int cityId) {
    return RESTGet(getClientsURI(cityId)+"robots");
  }

  public static ClientResponse RESTDeleteRobot(int cityId, int id) {
    return RESTDelete(getRobotsURI(cityId)+"remove/"+id);
  }

  public static ClientResponse RESTGetAvgLastNByRobotId(int cityId, int id, int n) {
    return RESTGet(getClientsURI(cityId)+"pollution/avg_id_n/"+id+"/"+n);
  }

  public static ClientResponse RESTGetAvgBetweenTimestamps(int cityId, long t1, long t2) {
    return RESTGet(getClientsURI(cityId)+"pollution/avg_t1_t2/"+t1+"/"+t2);
  }
}
