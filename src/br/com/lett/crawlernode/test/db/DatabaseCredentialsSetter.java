package br.com.lett.crawlernode.test.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.database.DBCredentials;
import br.com.lett.crawlernode.database.MongoCredentials;
import br.com.lett.crawlernode.database.MongoCredentialsSetter;
import br.com.lett.crawlernode.database.PostgresCredentials;
import br.com.lett.crawlernode.database.PostgresCredentialsSetter;
import br.com.lett.crawlernode.security.DataCipher;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class DatabaseCredentialsSetter {

	private static final Logger logger = LoggerFactory.getLogger(DatabaseCredentialsSetter.class);

	public static final String PANEL 	= "panel";
	public static final String INSIGHTS = "insights";
	public static final String IMAGES 	= "images";

	private String user;

	private static String key = null;

	public DatabaseCredentialsSetter(String user) {
		this.user = user;
	}

	public DBCredentials setDatabaseCredentials() {

		MongoCredentials mongoCredentials = new MongoCredentials();
		PostgresCredentials postgresCredentials = new PostgresCredentials();

		// Creating a data cipher
		DataCipher dataCipher = new DataCipher();

		try {

			// mongo insights
			MongoCredentialsSetter.getMongoInsightsParameters(dataCipher, key, user, INSIGHTS, mongoCredentials);

			// mongo panel
			MongoCredentialsSetter.getMongoPanelParameters(dataCipher, key, user, PANEL, mongoCredentials);

			// mongo images
			MongoCredentialsSetter.getMongoImagesParameters(dataCipher, key, user, IMAGES, mongoCredentials);

			// postgres
			PostgresCredentialsSetter.getPostgresParameters(dataCipher, key, postgresCredentials);

		} catch (Exception e) {
			Logging.printLogError(logger, "Error during database credentials deciphering operation.");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}


		return new DBCredentials(mongoCredentials, postgresCredentials);
	}

	public DBCredentials setDatabaseCredentialsTest() {

		MongoCredentials mongoCredentials = new MongoCredentials();
		PostgresCredentials postgresCredentials = new PostgresCredentials();

		// Creating a data cipher
		DataCipher dataCipher = new DataCipher();

		try {

			// mongo insights
			MongoCredentialsSetter.getMongoInsightsParameters(dataCipher, key, user, INSIGHTS, mongoCredentials);

			// mongo panel
			MongoCredentialsSetter.getMongoPanelParameters(dataCipher, key, user, PANEL, mongoCredentials);

			// mongo images
			MongoCredentialsSetter.getMongoImagesParameters(dataCipher, key, user, IMAGES, mongoCredentials);

			// postgres
			PostgresCredentialsSetter.getPostgresParameters(dataCipher, key, postgresCredentials);

		} catch (Exception e) {
			Logging.printLogError(logger, "Error during database credentials deciphering operation.");
			Logging.printLogError(logger, CommonMethods.getStackTraceString(e));
		}


		return new DBCredentials(mongoCredentials, postgresCredentials);
	}

}
