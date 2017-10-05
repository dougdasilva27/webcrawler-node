package br.com.lett.crawlernode.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.ReplicaSetStatus;
import com.mongodb.ServerAddress;

import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import comunication.MongoDB;
import comunication.PostgresSQL;
import credentials.models.DBCredentials;
import credentials.models.MongoCredentials;


public class DatabaseManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseManager.class);

	public MongoDB connectionPanel;
	public MongoDB connectionFrozen;
	public PostgresSQL connectionPostgreSQL;

	public DatabaseManager(DBCredentials credentials) {
		setMongoPanel(credentials);
		setMongoFrozen(credentials);
		
		connectionPostgreSQL = new PostgresSQL();
		
		try {
			connectionPostgreSQL.openConnection(credentials.getPostgresCredentials());
			Logging.printLogDebug(LOGGER, "Connection with database PostgreSQL performed successfully!");
		} catch (Exception e) {
			Logging.printLogError(LOGGER, "Error establishing connection with PostgreSQL.");
			Logging.printLogError(LOGGER, CommonMethods.getStackTraceString(e));
			System.exit(0);
		}
	}

	private void setMongoPanel(DBCredentials credentials) {
		MongoCredentials credentialsPanel = credentials.getMongoPanelCredentials();
		connectionPanel = new MongoDB();

		try {
			connectionPanel.openConnection(credentialsPanel);
			Logging.printLogDebug(LOGGER, "Connection with database Mongo Panel performed successfully!");
			
			ReplicaSetStatus replicaSetStatus = connectionPanel.getReplicaSetStatus();
			if ( replicaSetStatus != null ) {
				Logging.printLogDebug(LOGGER, "Connection mode: multiple");
				Logging.printLogDebug(LOGGER, replicaSetStatus.toString());
			} else {
				Logging.printLogDebug(LOGGER, "Connection mode: single");
				ServerAddress masterServerAddress = connectionPanel.getServerAddress();
				Logging.printLogDebug(LOGGER, masterServerAddress.toString());
			}
			
		} catch (Exception e) {
			Logging.printLogError(LOGGER, "Erro ao conectar com o Mongo Panel.");
			Logging.printLogError(LOGGER, CommonMethods.getStackTraceString(e));
		}
	}
	
	private void setMongoFrozen(DBCredentials credentials) {
		MongoCredentials credentialsFrozen = credentials.getMongoFrozenCredentials();
		connectionFrozen = new MongoDB();

		try {
			connectionFrozen.openConnection(credentialsFrozen);
			Logging.printLogDebug(LOGGER, "Connection with database Mongo Frozen performed successfully!");
			
			ReplicaSetStatus replicaSetStatus = connectionFrozen.getReplicaSetStatus();
			if ( replicaSetStatus != null ) {
				Logging.printLogDebug(LOGGER, "Connection mode: multiple");
				Logging.printLogDebug(LOGGER, replicaSetStatus.toString());
			} else {
				Logging.printLogDebug(LOGGER, "Connection mode: single");
				ServerAddress masterServerAddress = connectionFrozen.getServerAddress();
				Logging.printLogDebug(LOGGER, masterServerAddress.toString());
			}
			
		} catch (Exception e) {
			Logging.printLogError(LOGGER, "Erro ao conectar com o Mongo Frozen.");
			Logging.printLogError(LOGGER, CommonMethods.getStackTraceString(e));
		}
	}

}
