package adminserver;

import java.util.ArrayList;
import java.util.List;

import adminserver.REST.AdminServerThread;
import adminserver.REST.beans.InsertRobotBean;
import adminserver.REST.beans.RobotBean;
import adminserver.statistics.Statistics;
import utils.City;
import utils.District;
import utils.Position;
import utils.Config;

/**
 * The Administrator Server collects the IDs of the cleaning robots registered
 * to the system and also receives from them (through MQTT) the air pollution 
 * levels of Greenfield. This information will be then queried by the
 * administrators of the system (Administrator Client). Thus, this server has
 * to provide different REST interfaces for: (1) managing the robot network 
 * (2) enabling the administrators to execute queries on the air pollution levels
 */
public class AdministratorServer {
	private City city;
	private List<RegisteredRobot> registeredRobots;
	private Statistics statistics;
	private static AdministratorServer instance = null;

	private Thread httpServerThread;

	private AdministratorServer(int cityId) {
		// check if cityId is in the list of cities
		if (!City.isValidCityId(cityId)) {
			throw new IllegalArgumentException("Invalid cityId");
		}

		this.city = City.getCityById(cityId);
		this.registeredRobots = new ArrayList<RegisteredRobot>();

		this.startHttpServer();
		this.statistics = new Statistics(this.city);
	}
	
	private void startHttpServer() {
		AdminServerThread admin = new AdminServerThread(this.city);
		Thread adminThread = new Thread(admin);
		this.httpServerThread = adminThread;
		adminThread.start();
	}

	public static AdministratorServer getInstance(int cityId) {
		if (instance == null) {
			instance = new AdministratorServer(cityId);
		}
		if (instance.city.getId() != cityId) {
			throw new IllegalArgumentException("Invalid cityId");
		}
		return instance;
	}
	public static AdministratorServer getInstance() {
		if (instance == null) {
			throw new IllegalStateException("AdministratorServer not initialized");
		}
		return instance;
	}

	public City getCity() {
		return this.city;
	}

	/**
	 * The server has to store the following information for each robot joining Greenfield:
	 * ID, IP address (i.e., localhost), The port number on which it is available to handle 
	 * communications with the other robots.<p>
	 * Moreover, the server is in charge of assigning to each joining robot a random
	 * position in one of the districts of Greenfield (positions in Greenfield are
	 * expressed as the Cartesian coordinates of a smart city’s grid cell). 
	 * Note that, there can be more robots in the same grid cell of the smart city. The
	 * district must be chosen so that the cleaning robots are uniformly distributed
	 * among the districts. For instance, if there are 2 robots in District 1, 1 robot
	 * in Districts 2 and 3, and no robots in District 4, the next robot joining
	 * Greenfield should be placed in District 4. A robot can be added to the
	 * network only if there are no other robots with the same identifier. If the
	 * insertion succeeds, the Administrator Server returns to the cleaning robot
	 * (1) the starting position in Greenfield of the robot (2)the list of robots
	 * already located in the smart city, specifying for each of them the related 
	 * ID, the IP address, and the port number for communication
	 * @param robotBean
	 */
	public synchronized InsertRobotBean addRobot(RobotBean robotBean) throws IllegalArgumentException {
		testThreadSleep("addRobot " + robotBean.getId());


		// check if robot is already registered
		for (RegisteredRobot registeredRobot : this.registeredRobots) {
			if (registeredRobot.getId() == robotBean.getId()) {
				System.out.println("AdministratorServer: robot with id " + robotBean.getId() + " already registered");
				throw new IllegalArgumentException("Robot with id " + robotBean.getId() + " already registered");
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
		RegisteredRobot newRobot = new RegisteredRobot(robotBean.getId(),
				robotBean.getIpAddress(), robotBean.getPortNumber(), startPosition, district);
		district.addRobot(newRobot);
		this.registeredRobots.add(newRobot);

		// update statistics
		// List<Integer> validRobotIds = new ArrayList<Integer>();
		// for (RegisteredRobot registeredRobot : this.registeredRobots) {
		// 	validRobotIds.add(registeredRobot.getId());
		// }
		// this.statistics.setValidRobotIds(validRobotIds);

		// return the list of robots already present in Greenfield
		List<RobotBean> robotBeans = new ArrayList<RobotBean>();
		for (RegisteredRobot registeredRobot : this.registeredRobots) {
			if (registeredRobot.getId() != robotBean.getId()) {
				robotBeans.add(new RobotBean(registeredRobot.getId(),
						registeredRobot.getIpAddress(), registeredRobot.getPort()));
			}
		}

		System.out.println("AdministratorServer: Successfully registered robot with id " + robotBean.getId());
		printRegisteredRobots();
		return new InsertRobotBean(startPosition.getX(), startPosition.getY(), robotBeans);
	}

	public synchronized List<RobotBean> getRobots() {
		testThreadSleep("getRobots");

		List<RobotBean> robotBeans = new ArrayList<RobotBean>();
		for (RegisteredRobot registeredRobot : this.registeredRobots) {
			robotBeans.add(new RobotBean(registeredRobot.getId(),
					registeredRobot.getIpAddress(), registeredRobot.getPort()));
		}
		return robotBeans;
	}

	/**
	 * Whenever a cleaning robot asks the Administrator Server to leave the system, 
	 * the server has to remove it from the data structure representing the
	 * smart city. Similarly, when one of the robots informs the Administrator
	 * Server that a certain robot left the system in an uncontrolled way (e.g.,
	 * for a crash), the server has to remove such a robot from its internal data
	 * structure.
	 * @param id
	 */
	public synchronized void removeRobotById(int id) throws IllegalArgumentException {
		testThreadSleep("removeRobotById " + id);

		for (RegisteredRobot registeredRobot : this.registeredRobots) {
			if (registeredRobot.getId() == id) {
				registeredRobot.getDistrict().removeRobot(id);
				this.registeredRobots.remove(registeredRobot);

				System.out.println("AdministratorServer: Successfully removed robot with id " + id);
				printRegisteredRobots();
				return;
			}
		}

		// update statistics
		// List<Integer> validRobotIds = new ArrayList<Integer>();
		// for (RegisteredRobot registeredRobot : this.registeredRobots) {
		// 	validRobotIds.add(registeredRobot.getId());
		// }
		// this.statistics.setValidRobotIds(validRobotIds);

		throw new IllegalArgumentException("Robot with id " +id+ " does not exists");
	}

	public Statistics getStatistics() {
		return this.statistics;
	}

	public synchronized String getCityRepresentation() {
		return this.city.getRepresentation();
	}

	private void printRegisteredRobots() {
		String res = "";
		for (RegisteredRobot registeredRobot : this.registeredRobots) {
			res += registeredRobot.getId() + " ";
		}
		System.out.println("AdministratorServer: Registered robots [ " + res + "]");
	}

	private void testThreadSleep(String msg) {
		if (Config.RESOURCE_THREAD_SLEEP <= 0) {
			return;
		}
		System.out.println("[Thread start sleep] Admin: " + msg);
		try {
			Thread.sleep(Config.RESOURCE_THREAD_SLEEP*1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("[Thread end sleep] Admin: " + msg);
	}

}
