package br.com.lett.crawlernode.database;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import credentials.DBCredentialsSetter;
import credentials.models.DBCredentials;

public class DatabaseCredentialsSetter {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseCredentialsSetter.class);

  private DatabaseCredentialsSetter() {
    super();
  }

  public static DBCredentials setCredentials() throws Exception {
    DBCredentialsSetter st = new DBCredentialsSetter();

    List<String> databases = new ArrayList<>();
    databases.add(DBCredentials.MONGO_INSIGHTS);
    databases.add(DBCredentials.MONGO_FROZEN);
    databases.add(DBCredentials.MONGO_FETCHER);
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
      Logging.printLogError(LOGGER, CommonMethods.getStackTraceString(e));
      System.exit(0);
    }

    return new DBCredentials();
  }
}
