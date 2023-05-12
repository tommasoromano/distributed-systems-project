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
  
  /**
   * http://city.getHost():city.getPort()/robots/city.getId()/
   * @param city
   * @return
   */
  public static String getBaseURI(int cityId) {
    City city = City.getCityById(cityId);
    return "http://"+city.getHost()+":"+city.getPort()+"/robots/"+city.getId()+"/";
  }

  private static WebResource createResource(String url) {
    ClientConfig config = new DefaultClientConfig();
    config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    Client client = Client.create(config);
    return client.resource(url);
  }

  public static ClientResponse RESTPost(String url, Form form) {
    
    WebResource resource = createResource(url);

    System.out.println("Making REST POST Request: "
      +"\n\tResource: " + resource.getURI()
      +"\n\tForm:     " + form);
    
    ClientResponse response = resource
    .type(MediaType.APPLICATION_FORM_URLENCODED)
    .accept(MediaType.APPLICATION_JSON)
    .post(ClientResponse.class, form);

    System.out.println("Receiveing Response REST POST: "
      +"\n\tResource: " + resource.getURI()
      +"\n\tForm:     " + form
      +"\n\tResponse: " + response);

    return response;
  }

  public static ClientResponse RESTGet(String url) {
    
    WebResource resource = createResource(url);

    System.out.println("Making REST GET Request: "
      +"\n\tResource: " + resource.getURI());
    
    ClientResponse response = resource
    .type(MediaType.APPLICATION_FORM_URLENCODED)
    .accept(MediaType.APPLICATION_JSON)
    .get(ClientResponse.class);

    System.out.println("Receiveing Response REST GET: "
      +"\n\tResource: " + resource.getURI()
      +"\n\tResponse: " + response);

    return response;
  }

  public static ClientResponse RESTDelete(String url) {
    
    WebResource resource = createResource(url);

    System.out.println("Making REST DELETE Request: "
      +"\n\tResource: " + resource.getURI());
    
    ClientResponse response = resource
    .type(MediaType.APPLICATION_FORM_URLENCODED)
    .accept(MediaType.APPLICATION_JSON)
    .delete(ClientResponse.class);

    System.out.println("Receiveing Response REST DELETE: "
      +"\n\tResource: " + resource.getURI()
      +"\n\tResponse: " + response);

    return response;
  }

  public static ClientResponse RESTPostRobot(int cityId, int id, String ipAddress, int portNumber) {
    Form form = new Form();
    form.add("id", id + "");
    form.add("ipAddress", "localhost");
    form.add("portNumber", ((int)Math.floor(Math.random() * (9999 - 1000 + 1) + 1000)) + "");

    return RESTPost(getBaseURI(cityId)+"insert", form);
  }

  public static ClientResponse RESTGetAllRobots(int cityId) {
    return RESTGet(getBaseURI(cityId)+"robots");
  }

  public static ClientResponse RESTDeleteRobot(int cityId, int id) {
    return RESTDelete(getBaseURI(cityId)+"remove/"+id);
  }

  public static ClientResponse RESTGetAvgLastNByRobotId(int cityId, int id, int n) {
    return RESTGet(getBaseURI(cityId)+"pollution/avg_id_n/"+id+"/"+n);
  }

  public static ClientResponse RESTGetAvgBetweenTimestamps(int cityId, long t1, long t2) {
    return RESTGet(getBaseURI(cityId)+"pollution/avg_t1_t2/"+t1+"/"+t2);
  }
}
