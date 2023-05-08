package testers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import adminserver.AdministratorServer;
import adminserver.City;
import adminserver.REST.beans.Robot;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.representation.Form;

public class AdminClientRESTTester {

    @Test
    public void testConcurrentCalls() throws InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(20);

        for (int i = 0; i < 20; i++) {
            final int j = i;
            executor.submit(() -> {
                try {
                    Thread.sleep((long) (Math.random() * 100));
                    int randAction = ((int)Math.floor(Math.random() * (2 - 0 + 1) + 0));
                    if (randAction == 0) {
                        randomInsert(((int)Math.floor(Math.random() * (3 - 1 + 1) + 1)));
                    } else if (randAction == 1) {
                        getAllRobots();
                    } else if (randAction == 2) {
                        randomRemove(((int)Math.floor(Math.random() * (3 - 1 + 1) + 1)));
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }

    @Test
    public void squentialTest() throws InterruptedException {

        // Create multiple threads that access the shared resource
        Thread thread1 = new Thread(() -> AdministratorServer.getInstance(City.greenfieldCity.getId()).addRobot(new Robot(1, "", 0)));
        Thread thread2 = new Thread(() -> AdministratorServer.getInstance(City.greenfieldCity.getId()).addRobot(new Robot(1, "", 0)));
        Thread thread3 = new Thread(() -> AdministratorServer.getInstance(City.greenfieldCity.getId()).addRobot(new Robot(1, "", 0)));
        Thread thread4 = new Thread(() -> AdministratorServer.getInstance(City.greenfieldCity.getId()).addRobot(new Robot(1, "", 0)));
        Thread thread5 = new Thread(() -> AdministratorServer.getInstance(City.greenfieldCity.getId()).addRobot(new Robot(1, "", 0)));
        Thread thread6 = new Thread(() -> AdministratorServer.getInstance(City.greenfieldCity.getId()).addRobot(new Robot(1, "", 0)));

        // Start the threads
        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();
        thread5.start();
        thread6.start();

        // Wait for the threads to finish
        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();
        thread5.join();
        thread6.join();

        // Check that the shared resource was accessed synchronously
        assertEquals(1, AdministratorServer.getInstance(City.greenfieldCity.getId()).getRobots().size());
    }

  public void randomInsert(int id) {
    Client client = Client.create();
    WebResource resource = client.resource(getBaseURI()+"insert");
    Form form = new Form();
    form.add("id", id + "");
    form.add("ipAddress", "localhost");
    form.add("portNumber", ((int)Math.floor(Math.random() * (9999 - 1000 + 1) + 1000)) + "");
    System.out.println("POST Request: \n" + resource.getURI() + "\nForm:\n" + form);
    String response = resource.post(String.class, form);
    System.out.println("POST Response: \n" + response);
  }
  public void getAllRobots() {
    Client client = Client.create();
    WebResource resource = client.resource(getBaseURI()+"robots");
    System.out.println("GET Request: \n" + resource.getURI());
    String response = resource.get(String.class);
    System.out.println("GET Response: \n" + response);
  }
  public void randomRemove(int id) {
    Client client = Client.create();
    WebResource resource = client.resource(getBaseURI()+"remove/"+id);
    System.out.println("DELETE Request: \n" + resource.getURI());
    String response = resource.delete(String.class);
    System.out.println("DELETE Response: \n" + response);
  }
  public String getBaseURI() {
    return "http://localhost:6789/robots/1/";
  }
}