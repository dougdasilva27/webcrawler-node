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

	private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

	public MongoDB connectionPanel;
	public MongoDB connectionImages;
	public MongoDB connectionFrozen;
	public PostgresSQL connectionPostgreSQL;

	public DatabaseManager(DBCredentials credentials) {
		
		setMongoPanel(credentials);
		setMongoFrozen(credentials);
		setMongoImages(credentials);
		
		connectionPostgreSQL = new PostgresSQL();
		
		try {
			connectionPostgreSQL.openConnection(credentials.getPostgresCredentials());
			Logging.printLogDebug(logger, "Connection with database PostgreSQL performed successfully!");
		} catch (Exception e) {
			Logging.printLogError(logger, "Error establishing connection with PostgreSQL.");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
			System.exit(0);
		}
	}

	private void setMongoPanel(DBCredentials credentials) {
		MongoCredentials credentialsPanel = credentials.getMongoPanelCredentials();
		connectionPanel = new MongoDB();

		try {
			connectionPanel.openConnection(credentialsPanel);
			Logging.printLogDebug(logger, "Connection with database Mongo Panel performed successfully!");
			
			ReplicaSetStatus replicaSetStatus = connectionPanel.getReplicaSetStatus();
			if ( replicaSetStatus != null ) {
				Logging.printLogDebug(logger, "Connection mode: multiple");
				Logging.printLogDebug(logger, replicaSetStatus.toString());
			} else {
				Logging.printLogDebug(logger, "Connection mode: single");
				ServerAddress masterServerAddress = connectionPanel.getServerAddress();
				Logging.printLogDebug(logger, masterServerAddress.toString());
			}
			
		} catch (Exception e) {
			Logging.printLogError(logger, "Erro ao conectar com o Mongo Panel.");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}
	}
	
	private void setMongoFrozen(DBCredentials credentials) {
		MongoCredentials credentialsFrozen = credentials.getMongoFrozenCredentials();
		connectionFrozen = new MongoDB();

		try {
			connectionFrozen.openConnection(credentialsFrozen);
			Logging.printLogDebug(logger, "Connection with database Mongo Frozen performed successfully!");
			
			ReplicaSetStatus replicaSetStatus = connectionFrozen.getReplicaSetStatus();
			if ( replicaSetStatus != null ) {
				Logging.printLogDebug(logger, "Connection mode: multiple");
				Logging.printLogDebug(logger, replicaSetStatus.toString());
			} else {
				Logging.printLogDebug(logger, "Connection mode: single");
				ServerAddress masterServerAddress = connectionFrozen.getServerAddress();
				Logging.printLogDebug(logger, masterServerAddress.toString());
			}
			
		} catch (Exception e) {
			Logging.printLogError(logger, "Erro ao conectar com o Mongo Frozen.");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}
	}
	
	private void setMongoImages(DBCredentials credentials) {
		MongoCredentials credentialsImages = credentials.getMongoImagesCredentials();
		connectionImages = new MongoDB();

		try {
			connectionImages.openConnection(credentialsImages);
			Logging.printLogDebug(logger, "Connection with database Mongo Images performed successfully!");
			
			ReplicaSetStatus replicaSetStatus = connectionImages.getReplicaSetStatus();
			if ( replicaSetStatus != null ) {
				Logging.printLogDebug(logger, "Connection mode: multiple");
				Logging.printLogDebug(logger, replicaSetStatus.toString());
			} else {
				Logging.printLogDebug(logger, "Connection mode: single");
				ServerAddress masterServerAddress = connectionImages.getServerAddress();
				Logging.printLogDebug(logger, masterServerAddress.toString());
			}
			
		} catch (Exception e) {
			Logging.printLogError(logger, "Erro ao conectar com o Mongo Images.");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}
	}

}
