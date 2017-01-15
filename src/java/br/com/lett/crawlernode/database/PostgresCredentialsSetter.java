package br.com.lett.crawlernode.database;

import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.security.DataCipher;
import br.com.lett.crawlernode.util.Logging;
import ch.qos.logback.classic.Logger;

public class PostgresCredentialsSetter {
	
	private static Logger logger = (Logger) LoggerFactory.getLogger(PostgresCredentialsSetter.class);
	
	public static void getPostgresParameters(DataCipher dataCipher, String key, PostgresCredentials postgresCredentials) throws Exception {
		
		String username	= 	System.getenv("LETT_POSTGRES_USERNAME");
		String pass 	= 	System.getenv("LETT_POSTGRES_PASSWORD");
		String host		= 	System.getenv("LETT_POSTGRES_HOST");
		String port 	= 	System.getenv("LETT_POSTGRES_PORT");
		String database = 	System.getenv("LETT_POSTGRES_DATABASE");

		boolean mustExit = false;

		if( username == null 	|| 
			pass == null 		|| 
			host == null 		|| 
			port == null 		|| 
			database == null ) {

			if(username == null) {
				username = System.getenv("LETT_POSTGRES_USERNAME_ENCRYPTED");
				if(username != null) {
					username = dataCipher.decryptData(key, username);
				} else {
					Logging.printLogError(logger, "Variável de ambiente LETT_POSTGRES_USERNAME_ENCRYPTED não foi setada.");
					mustExit = true;
				}
			}
			if(pass == null) {
				pass = System.getenv("LETT_POSTGRES_PASSWORD_ENCRYPTED");
				if(pass != null) {
					pass = dataCipher.decryptData(key, pass); 
				} else {
					Logging.printLogError(logger, "Variável de ambiente LETT_POSTGRES_PASSWORD_ENCRYPTED não foi setada.");
					mustExit = true;
				}
			}
			if(host == null) {
				host = System.getenv("LETT_POSTGRES_HOST_ENCRYPTED");
				if(host != null) {
					host = dataCipher.decryptData(key, host);
				} else {
					Logging.printLogError(logger, "Variável de ambiente LETT_POSTGRES_HOST_ENCRYPTED não foi setada.");
					mustExit = true;
				}
			}
			if(port == null) {
				port = System.getenv("LETT_POSTGRES_PORT_ENCRYPTED");
				if(port != null) {
					port = dataCipher.decryptData(key, port);
				} else {
					Logging.printLogError(logger, "Variável de ambiente LETT_POSTGRES_PORT_ENCRYPTED não foi setada.");
					mustExit = true;
				}
			}
			if(database == null) {
				database = System.getenv("LETT_POSTGRES_DATABASE_ENCRYPTED");
				if(database != null) {
					database = dataCipher.decryptData(key, database);
				} else {
					Logging.printLogError(logger, "Variável de ambiente LETT_POSTGRES_DATABASE não foi setada.");
					mustExit = true;
				}
			}
		}
		
		postgresCredentials.setUsername(username);
		postgresCredentials.setPass(pass);
		postgresCredentials.setHost(host);
		postgresCredentials.setPort(port);
		postgresCredentials.setDatabase(database);

		if(mustExit) {
			System.exit(0);
		}

	}

}
