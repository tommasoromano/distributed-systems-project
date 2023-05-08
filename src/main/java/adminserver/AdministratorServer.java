package adminserver;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import adminserver.REST.beans.Robot;
import mqttbroker.StatisticsBroker;

/**
 * The Administrator Server is a single application that is in charge of:<p>
 * - Managing the insertion and removal of robots<p>
 * - Enable the Administrator Client to query statistics<p>
 * These services must be delivered via a REST architecture<p>
 * Moreover, it receives air pollution levels through MQTT<p>
 */
public class AdministratorServer {
	private City city;
	private List<RegisteredRobot> registeredRobots;
	private Statistics statistics;
	private static AdministratorServer instance = null;

	private AdministratorServer(int cityId) {
		// check if cityId is in the list of cities
		if (!City.isValidCityId(cityId)) {
			throw new IllegalArgumentException("Invalid cityId");
		}

		this.city = City.getCityById(cityId);
		this.registeredRobots = new ArrayList<RegisteredRobot>();

		this.startStatistics();
	}

	public synchronized static AdministratorServer getInstance(int cityId) {
		if (instance == null) {
			instance = new AdministratorServer(cityId);
		}
		if (instance.city.getId() != cityId) {
			throw new IllegalArgumentException("Invalid cityId");
		}
		return instance;
	}

	public synchronized void addRobot(Robot robot) {
		for (RegisteredRobot registeredRobot : this.registeredRobots) {
			if (registeredRobot.getId() == robot.getId()) {
				return;
			}
		}

		// get the number of robots for each district, 
		// and choose the district with the least robots
		List<District> districts = this.city.getDistricts();
		int[] robotsPerDistrict = new int[districts.size()];
		for (int i = 0; i < robotsPerDistrict.length; i++) {
			robotsPerDistrict[i] = districts.get(i).getRegisteredRobots().size();
		}
		int minIndex = 0;
		for (int i = 1; i < robotsPerDistrict.length; i++) {
			if (robotsPerDistrict[i] < robotsPerDistrict[minIndex]) {
				minIndex = i;
			}
		}
		District district = districts.get(minIndex);

		Position startPosition = district.getRandomPosition();
		RegisteredRobot newRobot = new RegisteredRobot(robot.getId(), 
				robot.getIpAddress(), robot.getPortNumber(), startPosition, district);
		district.addRobot(newRobot);
		this.registeredRobots.add(newRobot);
	}

	public synchronized List<Robot> getRobots() {
		List<Robot> robots = new ArrayList<Robot>();
		for (RegisteredRobot registeredRobot : this.registeredRobots) {
			robots.add(new Robot(registeredRobot.getId(), 
					registeredRobot.getIpAddress(), registeredRobot.getPort()));
		}
		return robots;
	}

	public synchronized void removeRobotById(int id) {
		for (RegisteredRobot registeredRobot : this.registeredRobots) {
			if (registeredRobot.getId() == id) {
				registeredRobot.getDistrict().removeRobot(id);
				this.registeredRobots.remove(registeredRobot);
				return;
			}
		}
	}
	private void startStatistics() {

		this.statistics = new Statistics();

		StatisticsBroker broker = new StatisticsBroker();
		Thread brokerThread = new Thread(broker);
		brokerThread.start();

		StatisticSubscriber subscriber = new StatisticSubscriber(this.city);
		Thread subscriberThread = new Thread(subscriber);
		subscriberThread.start();
	}
}
