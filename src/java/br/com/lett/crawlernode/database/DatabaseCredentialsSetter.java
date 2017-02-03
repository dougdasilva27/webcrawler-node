package br.com.lett.crawlernode.database;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.util.Logging;
import credentials.DBCredentialsSetter;
import credentials.MongoImagesCredentialsSetter;
import credentials.MongoInsightsCredentialsSetter;
import credentials.MongoPanelCredentialsSetter;
import credentials.PostgresCredentialsSetter;
import credentials.models.DBCredentials;

public class DatabaseCredentialsSetter {
	
	private static final Logger logger = LoggerFactory.getLogger(DatabaseCredentialsSetter.class);
	
	private DatabaseCredentialsSetter() {
		super();
	}

	public static DBCredentials setCredentials() {
		DBCredentialsSetter st = new DBCredentialsSetter();
		Map<String,String> enviroments = System.getenv();
		
		Map<String,String> credentials = new HashMap<>();
		Map<String,String> logs = new HashMap<>();
		
		setCredentialsPostgres(enviroments, credentials, logs);
		setCredentialsMongoInsights(enviroments, credentials, logs);
		setCredentialsMongoPanel(enviroments, credentials, logs);
		setCredentialsMongoImages(enviroments, credentials, logs);
		
		if(logs.size() > 0) {
			for(Entry<String, String> log : logs.entrySet()) {
				Logging.printLogError(logger, log.getKey() + " ou " + log.getValue() + " devem ser setadas.");
			}
			
			System.exit(0);
		}
		
		return st.setDatabaseCredentials(credentials);
	}
	
	private static void setCredentialsPostgres(Map<String,String> enviroments, Map<String,String> credentials, Map<String,String> logs) {
		if(enviroments.containsKey(PostgresCredentialsSetter.HOST_POSTGRES)) {
			credentials.put(PostgresCredentialsSetter.HOST_POSTGRES, enviroments.get(PostgresCredentialsSetter.HOST_POSTGRES));
		} else if(enviroments.containsKey(PostgresCredentialsSetter.HOST_POSTGRES_ENCRYPTED)) {
			credentials.put(PostgresCredentialsSetter.HOST_POSTGRES_ENCRYPTED, enviroments.get(PostgresCredentialsSetter.HOST_POSTGRES_ENCRYPTED));
		} else {
			logs.put(PostgresCredentialsSetter.HOST_POSTGRES, PostgresCredentialsSetter.HOST_POSTGRES_ENCRYPTED);
		}
		
		if(enviroments.containsKey(PostgresCredentialsSetter.PORT_POSTGRES)) {
			credentials.put(PostgresCredentialsSetter.PORT_POSTGRES, enviroments.get(PostgresCredentialsSetter.PORT_POSTGRES));
		} else if(enviroments.containsKey(PostgresCredentialsSetter.PORT_POSTGRES_ENCRYPTED)) {
			credentials.put(PostgresCredentialsSetter.PORT_POSTGRES_ENCRYPTED, enviroments.get(PostgresCredentialsSetter.PORT_POSTGRES_ENCRYPTED));
		} else {
			logs.put(PostgresCredentialsSetter.PORT_POSTGRES, PostgresCredentialsSetter.PORT_POSTGRES_ENCRYPTED);
		}
		
		if(enviroments.containsKey(PostgresCredentialsSetter.USERNAME_POSTGRES)) {
			credentials.put(PostgresCredentialsSetter.USERNAME_POSTGRES, enviroments.get(PostgresCredentialsSetter.USERNAME_POSTGRES));
		} else if(enviroments.containsKey(PostgresCredentialsSetter.USERNAME_POSTGRES_ENCRYPTED)) {
			credentials.put(PostgresCredentialsSetter.USERNAME_POSTGRES_ENCRYPTED, enviroments.get(PostgresCredentialsSetter.USERNAME_POSTGRES_ENCRYPTED));
		} else {
			logs.put(PostgresCredentialsSetter.USERNAME_POSTGRES, PostgresCredentialsSetter.USERNAME_POSTGRES_ENCRYPTED);
		}
		
		if(enviroments.containsKey(PostgresCredentialsSetter.PASSWORD_POSTGRES)) {
			credentials.put(PostgresCredentialsSetter.PASSWORD_POSTGRES, enviroments.get(PostgresCredentialsSetter.PASSWORD_POSTGRES));
		} else if(enviroments.containsKey(PostgresCredentialsSetter.PASSWORD_POSTGRES_ENCRYPTED)) {
			credentials.put(PostgresCredentialsSetter.PASSWORD_POSTGRES_ENCRYPTED, enviroments.get(PostgresCredentialsSetter.PASSWORD_POSTGRES_ENCRYPTED));
		} else {
			logs.put(PostgresCredentialsSetter.PASSWORD_POSTGRES, PostgresCredentialsSetter.PASSWORD_POSTGRES_ENCRYPTED);
		}
		
		if(enviroments.containsKey(PostgresCredentialsSetter.DATABASE_POSTGRES)) {
			credentials.put(PostgresCredentialsSetter.DATABASE_POSTGRES, enviroments.get(PostgresCredentialsSetter.DATABASE_POSTGRES));
		} else if(enviroments.containsKey(PostgresCredentialsSetter.DATABASE_POSTGRES_ENCRYPTED)) {
			credentials.put(PostgresCredentialsSetter.DATABASE_POSTGRES_ENCRYPTED, enviroments.get(PostgresCredentialsSetter.DATABASE_POSTGRES_ENCRYPTED));
		} else {
			logs.put(PostgresCredentialsSetter.DATABASE_POSTGRES, PostgresCredentialsSetter.DATABASE_POSTGRES_ENCRYPTED);
		}
	}
	
	private static void setCredentialsMongoPanel(Map<String,String> enviroments, Map<String,String> credentials, Map<String,String> logs) {
		if(enviroments.containsKey(MongoPanelCredentialsSetter.HOST_PANEL)) {
			credentials.put(MongoPanelCredentialsSetter.HOST_PANEL, enviroments.get(MongoPanelCredentialsSetter.HOST_PANEL));
		} else if(enviroments.containsKey(MongoPanelCredentialsSetter.HOST_PANEL_ENCRYPTED)) {
			credentials.put(MongoPanelCredentialsSetter.HOST_PANEL_ENCRYPTED, enviroments.get(MongoPanelCredentialsSetter.HOST_PANEL_ENCRYPTED));
		} else {
			logs.put(MongoPanelCredentialsSetter.HOST_PANEL, MongoPanelCredentialsSetter.HOST_PANEL_ENCRYPTED);
		}
		
		if(enviroments.containsKey(MongoPanelCredentialsSetter.PORT_PANEL)) {
			credentials.put(MongoPanelCredentialsSetter.PORT_PANEL, enviroments.get(MongoPanelCredentialsSetter.PORT_PANEL));
		} else if(enviroments.containsKey(MongoPanelCredentialsSetter.PORT_PANEL_ENCRYPTED)) {
			credentials.put(MongoPanelCredentialsSetter.PORT_PANEL_ENCRYPTED, enviroments.get(MongoPanelCredentialsSetter.PORT_PANEL_ENCRYPTED));
		} else {
			logs.put(MongoPanelCredentialsSetter.PORT_PANEL, MongoPanelCredentialsSetter.PORT_PANEL_ENCRYPTED);
		}
		
		if(enviroments.containsKey(MongoPanelCredentialsSetter.USERNAME_PANEL)) {
			credentials.put(MongoPanelCredentialsSetter.USERNAME_PANEL, enviroments.get(MongoPanelCredentialsSetter.USERNAME_PANEL));
		} else if(enviroments.containsKey(MongoPanelCredentialsSetter.USERNAME_PANEL_ENCRYPTED)) {
			credentials.put(MongoPanelCredentialsSetter.USERNAME_PANEL_ENCRYPTED, enviroments.get(MongoPanelCredentialsSetter.USERNAME_PANEL_ENCRYPTED));
		} else {
			logs.put(MongoPanelCredentialsSetter.USERNAME_PANEL, MongoPanelCredentialsSetter.USERNAME_PANEL_ENCRYPTED);
		}
		
		if(enviroments.containsKey(MongoPanelCredentialsSetter.PASSWORD_PANEL)) {
			credentials.put(MongoPanelCredentialsSetter.PASSWORD_PANEL, enviroments.get(MongoPanelCredentialsSetter.PASSWORD_PANEL));
		} else if(enviroments.containsKey(MongoPanelCredentialsSetter.PASSWORD_PANEL_ENCRYPTED)) {
			credentials.put(MongoPanelCredentialsSetter.PASSWORD_PANEL_ENCRYPTED, enviroments.get(MongoPanelCredentialsSetter.PASSWORD_PANEL_ENCRYPTED));
		} else {
			logs.put(MongoPanelCredentialsSetter.PASSWORD_PANEL, MongoPanelCredentialsSetter.PASSWORD_PANEL_ENCRYPTED);
		}
		
		if(enviroments.containsKey(MongoPanelCredentialsSetter.DATABASE_PANEL)) {
			credentials.put(MongoPanelCredentialsSetter.DATABASE_PANEL, enviroments.get(MongoPanelCredentialsSetter.DATABASE_PANEL));
		} else if(enviroments.containsKey(MongoPanelCredentialsSetter.DATABASE_PANEL_ENCRYPTED)) {
			credentials.put(MongoPanelCredentialsSetter.DATABASE_PANEL_ENCRYPTED, enviroments.get(MongoPanelCredentialsSetter.DATABASE_PANEL_ENCRYPTED));
		} else {
			logs.put(MongoPanelCredentialsSetter.DATABASE_PANEL, MongoPanelCredentialsSetter.DATABASE_PANEL_ENCRYPTED);
		}
	}
	
	private static void setCredentialsMongoInsights(Map<String,String> enviroments, Map<String,String> credentials, Map<String,String> logs) {
		if(enviroments.containsKey(MongoInsightsCredentialsSetter.HOST_INSIGHTS)) {
			credentials.put(MongoInsightsCredentialsSetter.HOST_INSIGHTS, enviroments.get(MongoInsightsCredentialsSetter.HOST_INSIGHTS));
		} else if(enviroments.containsKey(MongoInsightsCredentialsSetter.HOST_INSIGHTS_ENCRYPTED)) {
			credentials.put(MongoInsightsCredentialsSetter.HOST_INSIGHTS_ENCRYPTED, enviroments.get(MongoInsightsCredentialsSetter.HOST_INSIGHTS_ENCRYPTED));
		} else {
			logs.put(MongoInsightsCredentialsSetter.HOST_INSIGHTS, MongoInsightsCredentialsSetter.HOST_INSIGHTS_ENCRYPTED);
		}
		
		if(enviroments.containsKey(MongoInsightsCredentialsSetter.PORT_INSIGHTS)) {
			credentials.put(MongoInsightsCredentialsSetter.PORT_INSIGHTS, enviroments.get(MongoInsightsCredentialsSetter.PORT_INSIGHTS));
		} else if(enviroments.containsKey(MongoInsightsCredentialsSetter.PORT_INSIGHTS_ENCRYPTED)) {
			credentials.put(MongoInsightsCredentialsSetter.PORT_INSIGHTS_ENCRYPTED, enviroments.get(MongoInsightsCredentialsSetter.PORT_INSIGHTS_ENCRYPTED));
		} else {
			logs.put(MongoInsightsCredentialsSetter.PORT_INSIGHTS, MongoInsightsCredentialsSetter.PORT_INSIGHTS_ENCRYPTED);
		}
		
		if(enviroments.containsKey(MongoInsightsCredentialsSetter.USERNAME_INSIGHTS)) {
			credentials.put(MongoInsightsCredentialsSetter.USERNAME_INSIGHTS, enviroments.get(MongoInsightsCredentialsSetter.USERNAME_INSIGHTS));
		} else if(enviroments.containsKey(MongoInsightsCredentialsSetter.USERNAME_INSIGHTS_ENCRYPTED)) {
			credentials.put(MongoInsightsCredentialsSetter.USERNAME_INSIGHTS_ENCRYPTED, enviroments.get(MongoInsightsCredentialsSetter.USERNAME_INSIGHTS_ENCRYPTED));
		} else {
			logs.put(MongoInsightsCredentialsSetter.USERNAME_INSIGHTS, MongoInsightsCredentialsSetter.USERNAME_INSIGHTS_ENCRYPTED);
		}
		
		if(enviroments.containsKey(MongoInsightsCredentialsSetter.PASSWORD_INSIGHTS)) {
			credentials.put(MongoInsightsCredentialsSetter.PASSWORD_INSIGHTS, enviroments.get(MongoInsightsCredentialsSetter.PASSWORD_INSIGHTS));
		} else if(enviroments.containsKey(MongoInsightsCredentialsSetter.PASSWORD_INSIGHTS_ENCRYPTED)) {
			credentials.put(MongoInsightsCredentialsSetter.PASSWORD_INSIGHTS_ENCRYPTED, enviroments.get(MongoInsightsCredentialsSetter.PASSWORD_INSIGHTS_ENCRYPTED));
		} else {
			logs.put(MongoInsightsCredentialsSetter.PASSWORD_INSIGHTS, MongoInsightsCredentialsSetter.PASSWORD_INSIGHTS_ENCRYPTED);
		}
		
		if(enviroments.containsKey(MongoInsightsCredentialsSetter.DATABASE_INSIGHTS)) {
			credentials.put(MongoInsightsCredentialsSetter.DATABASE_INSIGHTS, enviroments.get(MongoInsightsCredentialsSetter.DATABASE_INSIGHTS));
		} else if(enviroments.containsKey(MongoInsightsCredentialsSetter.DATABASE_INSIGHTS_ENCRYPTED)) {
			credentials.put(MongoInsightsCredentialsSetter.DATABASE_INSIGHTS_ENCRYPTED, enviroments.get(MongoInsightsCredentialsSetter.DATABASE_INSIGHTS_ENCRYPTED));
		} else {
			logs.put(MongoInsightsCredentialsSetter.DATABASE_INSIGHTS, MongoInsightsCredentialsSetter.DATABASE_INSIGHTS_ENCRYPTED);
		}
	}
	
	private static void setCredentialsMongoImages(Map<String,String> enviroments, Map<String,String> credentials, Map<String,String> logs) {
		if(enviroments.containsKey(MongoImagesCredentialsSetter.HOST_IMAGES)) {
			credentials.put(MongoImagesCredentialsSetter.HOST_IMAGES, enviroments.get(MongoImagesCredentialsSetter.HOST_IMAGES));
		} else if(enviroments.containsKey(MongoImagesCredentialsSetter.HOST_IMAGES_ENCRYPTED)) {
			credentials.put(MongoImagesCredentialsSetter.HOST_IMAGES_ENCRYPTED, enviroments.get(MongoImagesCredentialsSetter.HOST_IMAGES_ENCRYPTED));
		} else {
			logs.put(MongoImagesCredentialsSetter.HOST_IMAGES, MongoImagesCredentialsSetter.HOST_IMAGES_ENCRYPTED);
		}
		
		if(enviroments.containsKey(MongoImagesCredentialsSetter.PORT_IMAGES)) {
			credentials.put(MongoImagesCredentialsSetter.PORT_IMAGES, enviroments.get(MongoImagesCredentialsSetter.PORT_IMAGES));
		} else if(enviroments.containsKey(MongoImagesCredentialsSetter.PORT_IMAGES_ENCRYPTED)) {
			credentials.put(MongoImagesCredentialsSetter.PORT_IMAGES_ENCRYPTED, enviroments.get(MongoImagesCredentialsSetter.PORT_IMAGES_ENCRYPTED));
		} else {
			logs.put(MongoImagesCredentialsSetter.PORT_IMAGES, MongoImagesCredentialsSetter.PORT_IMAGES_ENCRYPTED);
		}
		
		if(enviroments.containsKey(MongoImagesCredentialsSetter.USERNAME_IMAGES)) {
			credentials.put(MongoImagesCredentialsSetter.USERNAME_IMAGES, enviroments.get(MongoImagesCredentialsSetter.USERNAME_IMAGES));
		} else if(enviroments.containsKey(MongoImagesCredentialsSetter.USERNAME_IMAGES_ENCRYPTED)) {
			credentials.put(MongoImagesCredentialsSetter.USERNAME_IMAGES_ENCRYPTED, enviroments.get(MongoImagesCredentialsSetter.USERNAME_IMAGES_ENCRYPTED));
		} else {
			logs.put(MongoImagesCredentialsSetter.USERNAME_IMAGES, MongoImagesCredentialsSetter.USERNAME_IMAGES_ENCRYPTED);
		}
		
		if(enviroments.containsKey(MongoImagesCredentialsSetter.PASSWORD_IMAGES)) {
			credentials.put(MongoImagesCredentialsSetter.PASSWORD_IMAGES, enviroments.get(MongoImagesCredentialsSetter.PASSWORD_IMAGES));
		} else if(enviroments.containsKey(MongoImagesCredentialsSetter.PASSWORD_IMAGES_ENCRYPTED)) {
			credentials.put(MongoImagesCredentialsSetter.PASSWORD_IMAGES_ENCRYPTED, enviroments.get(MongoImagesCredentialsSetter.PASSWORD_IMAGES_ENCRYPTED));
		} else {
			logs.put(MongoImagesCredentialsSetter.PASSWORD_IMAGES, MongoImagesCredentialsSetter.PASSWORD_IMAGES_ENCRYPTED);
		}
		
		if(enviroments.containsKey(MongoImagesCredentialsSetter.DATABASE_IMAGES)) {
			credentials.put(MongoImagesCredentialsSetter.DATABASE_IMAGES, enviroments.get(MongoImagesCredentialsSetter.DATABASE_IMAGES));
		} else if(enviroments.containsKey(MongoImagesCredentialsSetter.DATABASE_IMAGES_ENCRYPTED)) {
			credentials.put(MongoImagesCredentialsSetter.DATABASE_IMAGES_ENCRYPTED, enviroments.get(MongoImagesCredentialsSetter.DATABASE_IMAGES_ENCRYPTED));
		} else {
			logs.put(MongoImagesCredentialsSetter.DATABASE_IMAGES, MongoImagesCredentialsSetter.DATABASE_IMAGES_ENCRYPTED);
		}
	}

}
