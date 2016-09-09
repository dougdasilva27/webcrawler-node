package br.com.lett.crawlernode.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;

import br.com.lett.crawlernode.main.ExecutionParameters;
import br.com.lett.crawlernode.main.Main;
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

	public MongoClient mongoClientDataCrawler = null;
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

			/* ************************
			 * Production environment *
			 **************************/
			if ( Main.executionParameters != null && Main.executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_PRODUCTION) ) {
				this.setMongoPanel();
				this.setMongoInsights();
				this.setMongoImages();
			} 

			/* *************************
			 * Development environment *
			 ***************************/
			else {
				mongoClientBackendPanel = new MongoClient("localhost", 27017);
				mongoBackendPanel = mongoClientBackendPanel.getDatabase("panel");
				mongoBackendInsights = mongoClientBackendPanel.getDatabase("insights");
				mongoMongoImages = mongoClientBackendPanel.getDatabase("images");
			}

			Logging.printLogDebug(logger, "Successfully connected to Mongo!");
		} catch (Exception e) {
			Logging.printLogError(logger, "An error occurred when trying to connect to Mongo.");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}

	}

	/**
	 * Connect to databases when running crawler tests.
	 */
	public void connectTest() {		
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

			mongoClientBackendPanel = new MongoClient("localhost", 27017);
			mongoBackendPanel = mongoClientBackendPanel.getDatabase("panel");
			mongoBackendInsights = mongoClientBackendPanel.getDatabase("insights");
			mongoMongoImages = mongoClientBackendPanel.getDatabase("images");

			Logging.printLogDebug(logger, "Successfully connected to Mongo!");
		} catch (Exception e) {
			Logging.printLogError(logger, "An error occurred when trying to connect to Mongo.");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}

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

	public ResultSet runSqlExecute(String sql) throws SQLException {
		Statement sta = conn.createStatement();
		sta.executeUpdate(sql);
		return sta.getGeneratedKeys();
	}

	public Document fetchSeedDocument(String url, Integer market) {

		if(this.mongoBackendPanel != null) {

			try {
				MongoCollection<Document> seedCollection =  this.mongoBackendPanel.getCollection("Seed");

				FindIterable<Document> iterable = seedCollection.find(
						Filters.and(
								Filters.eq("url", url), 
								Filters.eq("market", market)
								)
						);
				return iterable.first();

			} catch (Exception e) {
				System.err.println("ERROR fetching Seed document to update logs!");
				Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
			}			
		}

		return null;

	}


	public void appendLogToSeedDocument(String seedId, BasicDBObject log) {

		if(this.mongoBackendPanel != null) {

			try {
				MongoCollection<Document> seedCollection =  this.mongoBackendPanel.getCollection("Seed");

				Document mod = new Document();
				mod.append("logs", log);

				Document updateQuery = new Document();
				updateQuery.append("$push", mod);

				Document filterQuery = new Document();
				filterQuery.append("_id", new ObjectId(seedId));

				UpdateResult updateResult = seedCollection.updateOne(filterQuery, updateQuery);

			} catch (Exception e) {
				//System.err.println("ERROR appending log to Seed document!");
				Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
			}			
		}

	}


	public void markSeedAsDone(String seedId) {
		System.out.println("markSeedAsDone: " + seedId);

		if(this.mongoBackendPanel != null) {

			try {
				MongoCollection<Document> seedCollection =  this.mongoBackendPanel.getCollection("Seed");

				Document mod = new Document();
				mod.append("status", "done");

				Document updateQuery = new Document();
				updateQuery.append("$set", mod);

				Document filterQuery = new Document();
				filterQuery.append("_id", new ObjectId(seedId));

				UpdateResult updateResult = seedCollection.updateOne(filterQuery, updateQuery);

			} catch (Exception e) {
				//System.err.println("ERROR appending log to Seed document!");
				Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
			}			
		}

	}

}
