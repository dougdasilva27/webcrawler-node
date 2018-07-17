package br.com.lett.crawlernode.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mongodb.ReplicaSetStatus;
import com.mongodb.ServerAddress;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import credentials.models.DBCredentials;
import credentials.models.MongoCredentials;
import credentials.models.MysqlCredentials;
import managers.MongoDB;
import managers.SupervisedMYSQL;
import managers.SupervisedPgSQL;


public class DatabaseManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseManager.class);

  public MongoDB connectionPanel;
  public MongoDB connectionFrozen;
  public MongoDB connectionFetcher;
  public SupervisedPgSQL connectionPostgreSQL;
  public SupervisedMYSQL connectionMySQL;

  public DatabaseManager(DBCredentials credentials) {
    setMongoPanel(credentials);
    setMongoFrozen(credentials);
    setMongoFetcher(credentials);

    try {
      connectionPostgreSQL = new SupervisedPgSQL(credentials.getPostgresCredentials());
      Logging.printLogDebug(LOGGER, "Connection with database PostgreSQL performed successfully!");
    } catch (Exception e) {
      Logging.printLogError(LOGGER, "Error establishing connection with PostgreSQL.");
      Logging.printLogError(LOGGER, CommonMethods.getStackTraceString(e));
      System.exit(0);
    }

    MysqlCredentials mysqlCredentials = credentials.getMysqlCredentials();
    if (mysqlCredentials.isValid()) {
      try {
        connectionMySQL = new SupervisedMYSQL(mysqlCredentials);
        Logging.printLogDebug(LOGGER, "Connection with database MYSQL performed successfully!");
      } catch (Exception e) {
        Logging.printLogWarn(LOGGER, "Error establishing connection with MYSQL.");
        Logging.printLogWarn(LOGGER, CommonMethods.getStackTraceString(e));
      }
    }
  }

  private void setMongoPanel(DBCredentials credentials) {
    MongoCredentials credentialsPanel = credentials.getMongoPanelCredentials();
    connectionPanel = new MongoDB();

    try {
      connectionPanel.openConnection(credentialsPanel);
      Logging.printLogDebug(LOGGER, "Connection with database Mongo Panel performed successfully!");

      ReplicaSetStatus replicaSetStatus = connectionPanel.getReplicaSetStatus();
      if (replicaSetStatus != null) {
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

      ReplicaSetStatus replicaSetStatus = connectionFrozen.getReplicaSetStatus();
      if (replicaSetStatus != null) {
        Logging.printLogDebug(LOGGER, "Connection mode: multiple");
        Logging.printLogDebug(LOGGER, replicaSetStatus.toString());
      } else {
        Logging.printLogDebug(LOGGER, "Connection mode: single");
        ServerAddress masterServerAddress = connectionFrozen.getServerAddress();
        Logging.printLogDebug(LOGGER, masterServerAddress.toString());
      }
      Logging.printLogDebug(LOGGER, "Connection with database Mongo Frozen performed successfully!");
    } catch (Exception e) {
      Logging.printLogError(LOGGER, "Erro ao conectar com o Frozen.");
      Logging.printLogError(LOGGER, CommonMethods.getStackTraceString(e));
    }
  }

  private void setMongoFetcher(DBCredentials credentials) {
    MongoCredentials credentialsFetcher = credentials.getMongoFetcherCredentials();
    connectionFetcher = new MongoDB();

    try {
      connectionFetcher.openConnection(credentialsFetcher);

      ReplicaSetStatus replicaSetStatus = connectionFetcher.getReplicaSetStatus();
      if (replicaSetStatus != null) {
        Logging.printLogDebug(LOGGER, "Connection mode: multiple");
        Logging.printLogDebug(LOGGER, replicaSetStatus.toString());
      } else {
        Logging.printLogDebug(LOGGER, "Connection mode: single");
        ServerAddress masterServerAddress = connectionFetcher.getServerAddress();
        Logging.printLogDebug(LOGGER, masterServerAddress.toString());
      }

      Logging.printLogDebug(LOGGER, "Connection with database Mongo Fetcher performed successfully!");
    } catch (Exception e) {
      Logging.printLogError(LOGGER, "Erro ao conectar com o Mongo Fetcher.");
      Logging.printLogError(LOGGER, CommonMethods.getStackTraceString(e));
    }
  }

}
