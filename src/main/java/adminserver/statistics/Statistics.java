package adminserver.statistics;

import java.util.ArrayList;
import java.util.List;

import utils.City;
import utils.Config;
import simulator.MeasurementRecord;

/**
 * The Administrator Server has to collect through MQTT the air pollution 
 * measurements sent by the cleaning robots of Greenfield. More specifically,
 * the Administrator Server assumes the role of the subscriber for the following
 * four MQTT topics: greenfield/pollution/district{i}.<p>
 * The air pollution measurements have to be stored in proper data structures
 * that will be used to perform subsequent analyses.
 */
public class Statistics {

	private StatisticsBroker statisticsBroker;
	private StatisticSubscriber statisticsSubscriber;
	private Thread statisticsBrokerThread;
	private Thread statisticsSubscriberThread;

	private StatisticsDB statisticsDB;

	private List<Integer> validRobotIds;

  public Statistics(City city) {

    StatisticsBroker broker = new StatisticsBroker();
		this.statisticsBroker = broker;
		Thread brokerThread = new Thread(broker);
		this.statisticsBrokerThread = brokerThread;
		brokerThread.start();

		this.statisticsDB = StatisticsDB.getInstance();

		StatisticSubscriber subscriber = new StatisticSubscriber(city);
		this.statisticsSubscriber = subscriber;
		Thread subscriberThread = new Thread(subscriber);
		this.statisticsSubscriberThread = subscriberThread;
		subscriberThread.start();

		this.validRobotIds = new ArrayList<Integer>();

  }

	public synchronized void addMeasurement(MeasurementRecord measurement) {
		testThreadSleep("addMeasurement "+ MeasurementRecord.toJson(measurement));
		// if (!this.validRobotIds.contains(measurement.getRobotId())) {
		// 	System.out.println("Statistics: received measurement from invalid robotId: " + measurement.getRobotId());
		// 	return;
		// }
		this.statisticsDB.addMeasurement(measurement);
	}

	public synchronized double getAvgLastNByRobotId(int robotId, int n) {
		testThreadSleep("getAvgLastNByRobotId "+ robotId + " " + n);
		// if (!this.validRobotIds.contains(robotId)) {
		// 	System.out.println("Statistics: received get_avg_last_n from invalid robotId: " + robotId);
		// 	return -1;
		// }
		return this.statisticsDB.getAvgLastNByRobotId(robotId, n);
	}

	public synchronized double getAvgBetweenTimestamps(long t1, long t2) {
		testThreadSleep("getAvgBetweenTimestamps "+ t1 + " " + t2);
		return this.statisticsDB.getAvgBetweenTimestamps(t1, t2);
	}
	public synchronized String toDBRepersentation() {
		testThreadSleep("toDBRepersentation");
		return this.statisticsDB.dbToString();
	}

	public synchronized void setValidRobotIds(List<Integer> validRobotIds) {
		testThreadSleep("setValidRobotIds");

		// System.out.println("Statistics: setting valid robot ids: " + validRobotIds);

		this.validRobotIds = validRobotIds;
		this.statisticsDB.removeRecordsNotInValidRobotIds(validRobotIds);
	}

	private void testThreadSleep(String msg) {
		if (Config.RESOURCE_THREAD_SLEEP <= 0) {
			return;
		}
		System.out.println("[test] sleeping... Statistics: " + msg);
		try {
			Thread.sleep(Config.RESOURCE_THREAD_SLEEP*1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("[test] Statistics: " + msg);
	}
}
