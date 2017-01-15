package br.com.lett.crawlernode.database;

import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.security.DataCipher;
import br.com.lett.crawlernode.util.Logging;
import ch.qos.logback.classic.Logger;

public class MongoCredentialsSetter {

	private static Logger logger = (Logger) LoggerFactory.getLogger(MongoCredentialsSetter.class);
	
	private static final String LETT_MONGO_PREFIX = "LETT_MONGO_";

	public static void getMongoInsightsParameters(DataCipher dataCipher, String key, String user, String database, MongoCredentials mongoCredentials) throws Exception {

		String tmpUser = user.toUpperCase();
		String tmpDatabase = database.toUpperCase();

		String insightsUser		= 	System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_USERNAME");
		String insightsPass 	= 	System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_PASSWORD");
		String insightsHost		= 	System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_HOST");
		String insightsPort 	= 	System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_PORT");
		String insightsDatabase = 	System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_DATABASE");

		boolean mustExit = false;

		if(insightsUser == null || insightsPass == null || insightsHost == null || insightsPort == null || insightsDatabase == null) {

			if(insightsUser == null) {
				insightsUser = System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_USERNAME_ENCRYPTED");
				if(insightsUser != null) {
					insightsUser = dataCipher.decryptData(key, insightsUser);

				} else {
					Logging.printLogError(logger, "Variável de ambiente LETT_MONGO_" + tmpDatabase + "_" + tmpUser + "_USERNAME_ENCRYPTED não foi setada.");
					mustExit = true;
				}
			}
			if(insightsPass == null) {
				insightsPass = System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_PASSWORD_ENCRYPTED");
				if(insightsPass != null) {
					insightsPass = dataCipher.decryptData(key, insightsPass); 

				} else {
					Logging.printLogError(logger, "Variável de ambiente LETT_MONGO_" + tmpDatabase + "_" + tmpUser + "_PASSWORD_ENCRYPTED não foi setada.");
					mustExit = true;
				}
			}
			if(insightsHost == null) {
				insightsHost = System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_HOST_ENCRYPTED");
				if(insightsHost != null) {
					insightsHost = dataCipher.decryptData(key, insightsHost);

				} else {
					Logging.printLogError(logger, "Variável de ambiente LETT_MONGO_" + tmpDatabase + "_" + tmpUser + "_HOST_ENCRYPTED não foi setada.");
					mustExit = true;
				}
			}
			if(insightsPort == null) {
				insightsPort = System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_PORT_ENCRYPTED");
				if(insightsPort != null) {
					insightsPort = dataCipher.decryptData(key, insightsPort);

				} else {
					Logging.printLogError(logger, "Variável de ambiente LETT_MONGO_" + tmpDatabase + "_" + tmpUser + "_PORT_ENCRYPTED não foi setada.");
					mustExit = true;
				}
			}
			if(insightsDatabase == null) {
				insightsDatabase = System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_DATABASE_ENCRYPTED");
				if(insightsDatabase != null) {
					insightsDatabase = dataCipher.decryptData(key, insightsDatabase);
				} else {
					Logging.printLogError(logger, "Variável de ambiente LETT_MONGO_" + tmpDatabase + "_" + tmpUser + "_DATABASE_ENCRYPTED não foi setada.");
					mustExit = true;
				}
			}
		}

		mongoCredentials.setMongoInsightsUser(insightsUser);
		mongoCredentials.setMongoInsightsPass(insightsPass);
		mongoCredentials.setMongoInsightsHost(insightsHost);
		mongoCredentials.setMongoInsightsPort(insightsPort);
		mongoCredentials.setMongoInsightsDatabase(insightsDatabase);

		if (mustExit) {
			System.exit(0);
		}

	}

	public static void getMongoPanelParameters(DataCipher dataCipher, String key, String user, String database, MongoCredentials mongoCredentials) throws Exception {

		String tmpUser = user.toUpperCase();
		String tmpDatabase = database.toUpperCase();

		String panelUser		= 	System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_USERNAME");
		String panelPass 		= 	System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_PASSWORD");
		String panelHost		= 	System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_HOST");
		String panelPort 		= 	System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_PORT");
		String panelDatabase 	= 	System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_DATABASE");

		boolean mustExit = false;

		if(panelUser == null || panelPass == null || panelHost == null || panelPort == null || panelDatabase == null) {

			if(panelUser == null) {
				panelUser = System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_USERNAME_ENCRYPTED");
				if(panelUser != null && key != null) {
					panelUser = dataCipher.decryptData(key, panelUser);
				} else {
					Logging.printLogError(logger, "Variável de ambiente LETT_MONGO_" + tmpDatabase + "_" + tmpUser + "_USERNAME_ENCRYPTED não foi setada.");
					mustExit = true;
				}
			}
			if(panelPass == null) {
				panelPass = System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_PASSWORD_ENCRYPTED");
				if(panelPass != null && key != null) {
					panelPass = dataCipher.decryptData(key, panelPass); 
				} else {
					Logging.printLogError(logger, "Variável de ambiente LETT_MONGO_" + tmpDatabase + "_" + tmpUser + "_PASSWORD_ENCRYPTED não foi setada.");
					mustExit = true;
				}
			}
			if(panelHost == null) {
				panelHost = System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_HOST_ENCRYPTED");
				if(panelHost != null && key != null) {
					panelHost = dataCipher.decryptData(key, panelHost);
				} else {
					Logging.printLogError(logger, "Variável de ambiente LETT_MONGO_" + tmpDatabase + "_" + tmpUser + "_HOST_ENCRYPTED não foi setada.");
					mustExit = true;
				}
			}
			if(panelPort == null) {
				panelPort = System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_PORT_ENCRYPTED");
				if(panelPort != null && key != null) {
					panelPort = dataCipher.decryptData(key, panelPort);
				} else {
					Logging.printLogError(logger, "Variável de ambiente LETT_MONGO_" + tmpDatabase + "_" + tmpUser + "_PORT_ENCRYPTED não foi setada.");
					mustExit = true;
				}
			}
			if(panelDatabase == null) {
				panelDatabase = System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_DATABASE_ENCRYPTED");
				if(panelDatabase != null && key != null) {
					panelDatabase = dataCipher.decryptData(key, panelDatabase);
				} else {
					Logging.printLogError(logger, "Variável de ambiente LETT_MONGO_" + tmpDatabase + "_" + tmpUser + "_DATABASE_ENCRYPTED não foi setada.");
					mustExit = true;
				}
			}
		}
		
		mongoCredentials.setMongoPanelUser(panelUser);
		mongoCredentials.setMongoPanelPass(panelPass);
		mongoCredentials.setMongoPanelHost(panelHost);
		mongoCredentials.setMongoPanelPort(panelPort);
		mongoCredentials.setMongoPanelDatabase(panelDatabase);

		if (mustExit) System.exit(0);

	}
	
	public static void getMongoImagesParameters(DataCipher dataCipher, String key, String user, String database, MongoCredentials mongoCredentials) throws Exception {

		String tmpUser = user.toUpperCase();
		String tmpDatabase = database.toUpperCase();

		String imagesUser		= 	System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_USERNAME");
		String imagesPass 		= 	System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_PASSWORD");
		String imagesHost		= 	System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_HOST");
		String imagesPort 		= 	System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_PORT");
		String imagesDatabase 	= 	System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_DATABASE");

		boolean mustExit = false;

		if(imagesUser == null || imagesPass == null || imagesHost == null || imagesPort == null || imagesDatabase == null) {

			if(imagesUser == null) {
				imagesUser = System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_USERNAME_ENCRYPTED");
				if(imagesUser != null && key != null) {
					imagesUser = dataCipher.decryptData(key, imagesUser);
				} else {
					Logging.printLogError(logger, "Variável de ambiente LETT_MONGO_" + tmpDatabase + "_" + tmpUser + "_USERNAME_ENCRYPTED não foi setada.");
					mustExit = true;
				}
			}
			if(imagesPass == null) {
				imagesPass = System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_PASSWORD_ENCRYPTED");
				if(imagesPass != null && key != null) {
					imagesPass = dataCipher.decryptData(key, imagesPass); 
				} else {
					Logging.printLogError(logger, "Variável de ambiente LETT_MONGO_" + tmpDatabase + "_" + tmpUser + "_PASSWORD_ENCRYPTED não foi setada.");
					mustExit = true;
				}
			}
			if(imagesHost == null) {
				imagesHost = System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_HOST_ENCRYPTED");
				if(imagesHost != null && key != null) {
					imagesHost = dataCipher.decryptData(key, imagesHost);
				} else {
					Logging.printLogError(logger, "Variável de ambiente LETT_MONGO_" + tmpDatabase + "_" + tmpUser + "_HOST_ENCRYPTED não foi setada.");
					mustExit = true;
				}
			}
			if(imagesPort == null) {
				imagesPort = System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_PORT_ENCRYPTED");
				if(imagesPort != null && key != null) {
					imagesPort = dataCipher.decryptData(key, imagesPort);
				} else {
					Logging.printLogError(logger, "Variável de ambiente LETT_MONGO_" + tmpDatabase + "_" + tmpUser + "_PORT_ENCRYPTED não foi setada.");
					mustExit = true;
				}
			}
			if(imagesDatabase == null) {
				imagesDatabase = System.getenv(LETT_MONGO_PREFIX + tmpDatabase + "_" + tmpUser + "_DATABASE_ENCRYPTED");
				if(imagesDatabase != null && key != null) {
					imagesDatabase = dataCipher.decryptData(key, imagesDatabase);
				} else {
					Logging.printLogError(logger, "Variável de ambiente LETT_MONGO_" + tmpDatabase + "_" + tmpUser + "_DATABASE_ENCRYPTED não foi setada.");
					mustExit = true;
				}
			}
		}
		
		mongoCredentials.setMongoImagesUser(imagesUser);
		mongoCredentials.setMongoImagesPass(imagesPass);
		mongoCredentials.setMongoImagesHost(imagesHost);
		mongoCredentials.setMongoImagesPort(imagesPort);
		mongoCredentials.setMongoImagesDatabase(imagesDatabase);

		if (mustExit) {
			System.exit(0);
		}

	}

}
