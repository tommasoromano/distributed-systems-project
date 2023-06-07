package testers;

import adminserver.REST.RESTutils;

import com.sun.jersey.api.client.ClientResponse;

import utils.City;

public class RobotTester {
  public static void main(String[] args) {
    System.out.println("RobotTester");

    for (int i = 0; i < 5; i++) {
      int ts = i;
      Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            // random sleep
            int rand = ((int)Math.floor(Math.random() * (5 - 0 + 1) + 0));
            int _id = 1;
            Thread.sleep(ts * 1000);
            if (ts == 0) {RESTutils.RESTPostRobot(City.greenfieldCity.getId(),_id, "", _id);}
            if (ts == 1) {RESTutils.RESTPostRobot(City.greenfieldCity.getId(),_id, "", _id);}
            if (ts == 2) {RESTutils.RESTDeleteRobot(City.greenfieldCity.getId(),_id);}
            if (ts == 3) {RESTutils.RESTPostRobot(City.greenfieldCity.getId(),_id, "", _id);}
          } catch (Exception e) {
            // e.printStackTrace();
          }
        }
      });
      t.start();
    }
  }
}
