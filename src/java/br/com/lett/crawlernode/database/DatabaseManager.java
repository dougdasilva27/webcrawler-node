package br.com.lett.crawlernode.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import comunication.MongoDB;
import comunication.PostgresSQL;
import credentials.models.DBCredentials;


public class DatabaseManager {

	private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

	public MongoDB connectionPanel;
	public MongoDB connectionInsights; 
	public MongoDB connectionImages;
	public PostgresSQL connectionPostgreSQL;

	public DatabaseManager(DBCredentials credentials) {
		connectionPanel = new MongoDB();
		connectionInsights = new MongoDB();
		connectionImages = new MongoDB();
		connectionPostgreSQL = new PostgresSQL();

		try {
			connectionPanel.openConnection(credentials.getMongoPanelCredentials());
			Logging.printLogDebug(logger, "Connection with database Mongo Panel performed successfully!");
		} catch (Exception e) {
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}

		try {
			connectionInsights.openConnection(credentials.getMongoInsightsCredentials());
			Logging.printLogDebug(logger, "Connection with database Mongo Insights performed successfully!");
		} catch (Exception e) {
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}

		try {
			connectionImages.openConnection(credentials.getMongoImagesCredentials());
			Logging.printLogDebug(logger, "Connection with database Mongo Frozen performed successfully!");
		} catch (Exception e) {
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}

		try {
			connectionPostgreSQL.openConnection(credentials.getPostgresCredentials());
			Logging.printLogDebug(logger, "Connection with database PostgreSQL performed successfully!");
		} catch (Exception e) {
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}
	}

	//	private void testMongoConnection() {
	//		mongoClientBackendPanel.getAddress();
	//		mongoClientBackendDashboard.getAddress();
	//		mongoClientMongoImages.getAddress();
	//	}


}
