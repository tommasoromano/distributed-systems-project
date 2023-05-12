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

  }

	public void addMeasurement(MeasurementRecord measurement) {
		this.statisticsDB.addMeasurement(measurement);
	}

	public double getAvgLastNByRobotId(int robotId, int n) {
		return this.statisticsDB.getAvgLastNByRobotId(robotId, n);
	}

	public double getAvgBetweenTimestamps(long t1, long t2) {
		return this.statisticsDB.getAvgBetweenTimestamps(t1, t2);
	}
	public String toDBRepersentation() {
		return this.statisticsDB.dbToString();
	}

}
