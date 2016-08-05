package br.com.lett.crawlernode.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.kernel.ExecutionParameters;
import br.com.lett.crawlernode.main.Main;
import br.com.lett.crawlernode.security.DataCipher;
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

		if ( Main.executionParameters.getEnvironment().equals(ExecutionParameters.ENVIRONMENT_PRODUCTION) ) { 
			key = dataCipher.fetchRemoteKey("https://s3.amazonaws.com/security-lett/lett");
		}

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
			Logging.printLogError(logger, e.getMessage());
		}
		
		
		return new DBCredentials(mongoCredentials, postgresCredentials);
	}

}
