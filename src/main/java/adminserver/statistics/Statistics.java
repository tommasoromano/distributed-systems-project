package adminserver.statistics;

import java.util.ArrayList;
import java.util.List;

import adminserver.AdministratorServer;
import simulator.Measurement;
import utils.City;

public class Statistics {

	private Thread statisticsBrokerThread;
	private Thread statisticsSubscriberThread;

	private StatisticsDB statisticsDB;

	private List<Integer> validRobotIds;

  public Statistics(City city) {

    StatisticsBroker broker = new StatisticsBroker();
		Thread brokerThread = new Thread(broker);
		this.statisticsBrokerThread = brokerThread;
		brokerThread.start();

		this.statisticsDB = StatisticsDB.getInstance();

		StatisticSubscriber subscriber = new StatisticSubscriber(city);
		Thread subscriberThread = new Thread(subscriber);
		this.statisticsSubscriberThread = subscriberThread;
		subscriberThread.start();

		this.validRobotIds = new ArrayList<Integer>();

  }

	public synchronized void addMeasurement(MeasurementRecord measurement) {
		if (!this.validRobotIds.contains(measurement.getRobotId())) {
			System.out.println("Statistics: received measurement from invalid robotId: " + measurement.getRobotId());
			return;
		}
		this.statisticsDB.addMeasurement(measurement);
	}

	public synchronized double getAvgLastNByRobotId(int robotId, int n) {
		if (!this.validRobotIds.contains(robotId)) {
			System.out.println("Statistics: received get_avg_last_n from invalid robotId: " + robotId);
			return -1;
		}
		return this.statisticsDB.getAvgLastNByRobotId(robotId, n);
	}

	public synchronized double getAvgBetweenTimestamps(long t1, long t2) {
		return this.statisticsDB.getAvgBetweenTimestamps(t1, t2);
	}
	public synchronized String toDBRepersentation() {
		return this.statisticsDB.dbToString();
	}

	public synchronized void setValidRobotIds(List<Integer> validRobotIds) {

		System.out.println("Statistics: setting valid robot ids: " + validRobotIds);

		this.validRobotIds = validRobotIds;
		this.statisticsDB.removeRecordsNotInValidRobotIds(validRobotIds);
	}

}
