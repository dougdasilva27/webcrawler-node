package br.com.lett.crawlernode.database;

import br.com.lett.crawlernode.database.managers.ElasticSearch;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import com.mongodb.ReplicaSetStatus;
import com.mongodb.ServerAddress;
import credentials.models.DBCredentials;
import credentials.models.ElasticCredentials;
import credentials.models.MongoCredentials;
import managers.MongoDB;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseManager {

   private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseManager.class);

   public MongoDB connectionFrozen;
   public MongoDB connectionFetcher;
   public ElasticSearch connectionElasticSearch;
   public DSLContext jooqPostgres = DSL.using(SQLDialect.POSTGRES);

   public DatabaseManager(DBCredentials credentials) {
     setMongoFrozen(credentials);
     setMongoFetcher(credentials);
   }

   public DatabaseManager(DBCredentials credentials, boolean isSessionSeeds) {
      if (isSessionSeeds){
         setElasticSearch(credentials);
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
   private void setElasticSearch(DBCredentials credentials) {
      ElasticCredentials credentialsElastic = credentials.getElasticCredentials();
      connectionElasticSearch = new ElasticSearch();

      try {
         connectionElasticSearch.connection(credentialsElastic);

         Logging.printLogDebug(LOGGER, "Connection with database Elastic performed successfully!");

      } catch (Exception e) {
         Logging.printLogError(LOGGER, "Error to connect with Elastic.");
         Logging.printLogError(LOGGER, CommonMethods.getStackTraceString(e));
      }
   }


}
