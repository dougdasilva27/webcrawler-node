package br.com.lett.crawlernode.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import br.com.lett.crawlernode.main.GlobalConfigurations;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import credentials.DBCredentialsSetter;
import credentials.models.DBCredentials;

/**
 * Eagle loading connection factory singleton. As we are sure that it will be used at some point
 * during the execution, we don't need to implement as a lazy loading instance. Plus as it will not
 * be lazy loaded, we don't need to deal with synchronization problems on the getInstance() method.
 * 
 * @author Samir Leão
 *
 */
public class JdbcConnectionFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(JdbcConnectionFactory.class);

  private static final String HIKARI_DATASOURCE_CLASS_NAME = "org.postgresql.ds.PGSimpleDataSource";
  private static final String APPLICATION_NAME = "crawler";

  /*
   * To make this a lazy loading instance just remove the final keyword
   */
  private static final JdbcConnectionFactory INSTANCE = new JdbcConnectionFactory();

  private HikariDataSource ds;

  private JdbcConnectionFactory() {
    DBCredentials credentials = setCredentials();

    Properties props = new Properties();
    props.setProperty("dataSourceClassName", HIKARI_DATASOURCE_CLASS_NAME);
    props.setProperty("dataSource.user", credentials.getPostgresCredentials().getUsername());
    props.setProperty("dataSource.password", credentials.getPostgresCredentials().getPass());
    props.setProperty("dataSource.databaseName", credentials.getPostgresCredentials().getDatabase());
    props.setProperty("dataSource.serverName", credentials.getPostgresCredentials().getHost());
    props.setProperty("dataSource.ApplicationName", APPLICATION_NAME);

    HikariConfig config = new HikariConfig(props);
    config.setMinimumIdle(GlobalConfigurations.executionParameters.getHikariCpMinIDLE());
    config.setMaximumPoolSize(GlobalConfigurations.executionParameters.getHikariCpMaxPoolSize());
    config.setValidationTimeout(GlobalConfigurations.executionParameters.getHikariCpValidationTimeout());
    config.setConnectionTimeout(GlobalConfigurations.executionParameters.getHikariCpConnectionTimeout());
    config.setIdleTimeout(GlobalConfigurations.executionParameters.getHikariCpIDLETimeout());

    try {
      ds = new HikariDataSource(config);
      Logging.printLogDebug(LOGGER, "Pool of PostgreSQL connections started successfully!");
    } catch (Exception e) {
      Logging.printLogError(LOGGER, "Error creating DataSource");
      Logging.printLogError(LOGGER, CommonMethods.getStackTrace(e));
      System.exit(1);
    }
  }

  public static JdbcConnectionFactory getInstance() {
    return INSTANCE;
  }

  /**
   * Retrieve a connection from the pool. After use, the client code must close the connection.
   * 
   * @return an instance of
   * @throws SQLException
   */
  public Connection getConnection() throws SQLException {
    return this.ds.getConnection();
  }

  public void close() {
    this.ds.close();
  }

  public static void closeResource(AutoCloseable r) {
    if (r != null) {
      try {
        r.close();

        if (r instanceof Connection) {
          Logging.printLogInfo(LOGGER, "Connection has been closed");
        }
      } catch (Exception e) {
        Logging.printLogError(LOGGER, CommonMethods.getStackTrace(e));
      }
    }
  }

  private DBCredentials setCredentials() {
    DBCredentialsSetter st = new DBCredentialsSetter();
    List<String> databases = new ArrayList<>();
    databases.add(DBCredentials.POSTGRES);
    try {
      DBCredentials credentials = st.setDatabaseCredentials(databases);
      List<String> logErrorsList = st.getLogErors();

      if (!logErrorsList.isEmpty()) {
        for (String log : logErrorsList) {
          Logging.printLogError(LOGGER, log);
        }

        System.exit(0);
      } else {
        return credentials;
      }

    } catch (Exception e) {
      Logging.printLogError(LOGGER, CommonMethods.getStackTrace(e));
      System.exit(1);
    }
    return new DBCredentials();
  }

}
