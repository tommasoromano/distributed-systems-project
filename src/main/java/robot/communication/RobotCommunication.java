package robot.communication;

import java.util.List;

import javax.ws.rs.core.Response;

import adminserver.REST.RESTutils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.representation.Form;

import adminclient.AdminClient;
import robot.Robot;
import utils.City;

public class RobotCommunication {
  
  private Robot robot;

  private MqttClient client;

  public RobotCommunication(Robot robot) {
    this.robot = robot;
  }

  /**
   * requires the robot to be initialized
   * @return
   */
  public boolean joinNetwork() {

    System.out.println("Robot "+this.robot.getId()+" is trying to join the network.");

    Form form = new Form();
    form.add("id", this.robot.getId() + "");
    form.add("ipAddress", this.robot.getIpAddress());
    form.add("portNumber", this.robot.getPortNumber() + "");

    ClientResponse response = RESTutils.RESTPost(RESTutils.getBaseURI(this.robot.getCity())+"insert", form);

    if (response.getStatus() != Response.Status.OK.getStatusCode()) {
      System.out.println("Robot "+this.robot.getId()+" failed to join the network.");
      return false;
    }

    System.out.println("This Robot "+this.robot.getId()+" joined the network.");

    try {
      List<Robot> robots = response.getEntity(new GenericType<List<Robot>>() {});
      for (Robot robot : robots) {
        System.out.println("This Robot "+this.robot.getId()+" received robot "+robot.getId()+" from the network.");
      }
    } catch (Exception e) {
      System.out.println("Error: "+e.getMessage());
      this.robot.disconnect();
      return false;
    }

    // create mqtt
    String broker = "tcp://localhost:1883";
    String clientId = MqttClient.generateClientId();
    try {
        this.client = new MqttClient(broker, clientId);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true); // false = the broker stores all subscriptions for the client and all missed messages for the client that subscribed with a Qos level 1 or 2

        // Connect the client
        System.out.println(clientId + " Connecting Broker " + broker);
        client.connect(connOpts);
        System.out.println(clientId + " Connected Broker " + broker);

    } catch (MqttException me ) {
        System.out.println("reason " + me.getReasonCode());
        System.out.println("msg " + me.getMessage());
        System.out.println("loc " + me.getLocalizedMessage());
        System.out.println("cause " + me.getCause());
        System.out.println("excep " + me);
        
        this.robot.disconnect();
    }

    return true;
  }

  public void sendMQTTMessage(String payload) {
    try {
      String topic = this.robot.getCity().getName().toLowerCase()+"/pollution/district1";
      MqttMessage message = new MqttMessage(payload.getBytes());
      message.setQos(2);
      client.publish(topic, message);
    } catch (MqttException e) {
      System.out.println("Error: "+e.getMessage());
    }
  }

}
