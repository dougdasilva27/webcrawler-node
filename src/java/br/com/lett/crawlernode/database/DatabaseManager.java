package br.com.lett.crawlernode.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;

import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;


public class DatabaseManager {

	private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

	/**
	 * Database credentials
	 */
	private MongoCredentials mongoCredentials;
	private PostgresCredentials postgresCredentials;

	/**
	 * Postgresql connection
	 */
	public Connection conn = null;

	//public MongoClient mongoClientDataCrawler = null;
	public MongoClient mongoClientBackendPanel = null;
	public MongoClient mongoClientBackendDashboard = null;
	public MongoClient mongoClientMongoImages = null;

	//	public MongoDatabase mongo_data_crawler = null;
	public MongoDatabase mongoBackendPanel = null;
	public MongoDatabase mongoBackendInsights = null;
	public MongoDatabase mongoMongoImages = null;

	public DatabaseManager(DBCredentials credentials) {
		this.mongoCredentials = credentials.getMongoCredentials();
		this.postgresCredentials = credentials.getPostgresCredentials();
	}

	/**
	 * Connect to databases when not testing crawler logic.
	 */
	public void connect() {		
		Logging.printLogDebug(logger, "Starting connection with databases...");

		// connect to postgres
		try {
			Class.forName("org.postgresql.Driver");
			String url = "jdbc:postgresql://" + postgresCredentials.getHost() + ":" + postgresCredentials.getPort() + "/" + postgresCredentials.getDatabase();
			conn = DriverManager.getConnection(url, postgresCredentials.getUsername(), postgresCredentials.getPass());

			Logging.printLogDebug(logger, "Successfully connected to Postgres!");
		} catch (SQLException e) {
			Logging.printLogError(logger, "An error occurred when trying to connect to Postgres.");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		} catch (ClassNotFoundException e) {
			Logging.printLogError(logger, "An error occurred when trying to connect to Postgres.");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}

		// connect to mongodb
		try {
			setMongoPanel();
			setMongoInsights();
			setMongoImages();

			testMongoConnection();			

			Logging.printLogDebug(logger, "Successfully connected to Mongo: " + mongoCredentials.getMongoPanelHost());
		} catch (Exception e) {
			Logging.printLogError(logger, "An error occurred when trying to connect to Mongo.");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}

	}
	
	private void testMongoConnection() {
		mongoClientBackendPanel.getAddress();
		mongoClientBackendDashboard.getAddress();
		mongoClientMongoImages.getAddress();
	}

	private void setMongoPanel() {
		String uriBackendPanel 	= 
				"mongodb://" + mongoCredentials.getMongoPanelUser() + 
				":" + mongoCredentials.getMongoPanelPass() + 
				"@" + mongoCredentials.getMongoPanelHost() + 
				":" + mongoCredentials.getMongoPanelPort() + 
				"/?authSource=" + 
				mongoCredentials.getMongoPanelDatabase();

		MongoClientURI uri_backend_panel = new MongoClientURI(uriBackendPanel);
		mongoClientBackendPanel = new MongoClient(uri_backend_panel);
		mongoBackendPanel = mongoClientBackendPanel.getDatabase(mongoCredentials.getMongoPanelDatabase());
	}

	private void setMongoInsights() {
		String uriBackendDashboard 	= 
				"mongodb://" + mongoCredentials.getMongoInsightsUser() + 
				":" + mongoCredentials.getMongoInsightsPass() + 
				"@" + mongoCredentials.getMongoInsightsHost() + 
				":" + mongoCredentials.getMongoInsightsPort() + 
				"/?authSource=" + 
				mongoCredentials.getMongoInsightsDatabase();

		MongoClientURI uri_backend_dashboard = new MongoClientURI(uriBackendDashboard);
		mongoClientBackendDashboard = new MongoClient(uri_backend_dashboard);
		mongoBackendInsights = mongoClientBackendDashboard.getDatabase(mongoCredentials.getMongoInsightsDatabase());
	}

	private void setMongoImages() {
		String uriMongoImages = 
				"mongodb://" + mongoCredentials.getMongoImagesUser() + 
				":" + mongoCredentials.getMongoImagesPass() + 
				"@" + mongoCredentials.getMongoImagesHost() + 
				":" + mongoCredentials.getMongoImagesPort() + 
				"/?authSource=" + 
				mongoCredentials.getMongoImagesDatabase();

		MongoClientURI uri_mongo_images = new MongoClientURI(uriMongoImages);
		mongoClientMongoImages = new MongoClient(uri_mongo_images);
		mongoMongoImages = mongoClientMongoImages.getDatabase(mongoCredentials.getMongoImagesDatabase());
	}

	public ResultSet runSqlConsult(String sql) throws SQLException {
		Statement sta = conn.createStatement();
		return sta.executeQuery(sql);
	}

	public void runSqlExecute(String sql) throws SQLException {
		Statement sta = conn.createStatement();
		sta.executeUpdate(sql);
	}

}
