package br.com.lett.crawlernode.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
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
	
	public DSLContext create;
	

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
			
			String userName = postgresCredentials.getUsername();
			String pass = postgresCredentials.getPass();
			
			// setting connection properties
			Properties connectionProperties = new Properties();
			
			connectionProperties.put("tcpKeepAlive", "true");	
			
			if (userName != null) {
				connectionProperties.put("user", userName);
			}
			
	        if (pass != null) {
	        	connectionProperties.put("password", pass);
	        }
	        
			conn = DriverManager.getConnection(url, connectionProperties);
			create = DSL.using(conn, SQLDialect.POSTGRES_9_4);
			
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
	
	/**
	 * Simple insert
	 * @param table
	 * @param fields
	 * @param values
	 */
	public void runInsertJooq(Table<?> table, Map<Field<?>, Object> insertMap){
		try {
			create.insertInto(table)
			.set(insertMap)  
			.execute();
		} catch (DataAccessException e) {
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}
	}
	
	/**
	 * Create a batch for insert
	 * 
	 * First pass the list of tables that will be inserted
	 * Second pass a map of the tables with their fields
	 * Third to pass a map of the tables with their values
	 * 
	 * Then is created a lits of Queries and batch is created and executed
	 * 
	 * @param tables
	 * @param fieldsMap
	 * @param valuesMap
	 */
	public void runBatchInsertJooq(List<Table<?>> tables, Map<Table<?>,Map<Field<?>, Object>> tableMap){
		try {
			List<Query> queries = new ArrayList<>();
			
			for(Table<?> table : tables){
				Map<Field<?>, Object> insertMap = tableMap.get(table);
				
				queries.add(create.insertInto(table)
				.set(insertMap)); 
			}
			
			create.batch(queries).execute();
			
		} catch (DataAccessException e) {
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}
	}
	
	/**
	 * Pass the id field to return its value 
	 * @param table
	 * @param fields
	 * @param values
	 * @param fieldReturning
	 * @return
	 */
	public Record runInsertJooqReturningID(Table<?> table,  Map<Field<?>, Object> insertMap, Field<?> fieldReturning){
		try {
			return create.insertInto(table)
			.set(insertMap)
			.returning(fieldReturning)
			.fetchOne();
		} catch (DataAccessException e) {
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}
		
		return null;
	}
	
	/**
	 * Simple update
	 * @param table
	 * @param updateMap (Map<Field(column), Object(value)>)
	 * @param conditions
	 */
	public void runUpdateJooq(Table<?> table, Map<Field<?>, Object> updateMap, List<Condition> conditions){
		try {			
			create.update(table)
			.set(updateMap)
			.where(conditions)
			.execute();
		} catch (DataAccessException e) {
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}
	}
	
	/**
	 * Simple select
	 * 
	 * if fields == null, will return all fileds of table
	 * @param table
	 * @param fields
	 * @param conditions
	 * @return
	 */
	public ResultSet runSelectJooq(Table<?> table, List<Field<?>> fields, List<Condition> conditions){
		if(fields != null){
			return create.select(fields).from(table).where(conditions).fetchResultSet();
		}
		
		return create.select().from(table).where(conditions).fetchResultSet();
	}

}
